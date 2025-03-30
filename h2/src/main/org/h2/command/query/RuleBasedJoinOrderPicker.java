package org.h2.command.query;

import java.util.*;
import org.h2.table.TableFilter;
import org.h2.engine.SessionLocal;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.condition.Comparison;
import org.h2.expression.condition.ConditionAndOr;
import org.h2.expression.condition.ConditionAndOrN;


public class RuleBasedJoinOrderPicker {
    final SessionLocal currentSession;
    final TableFilter[] filters;

    // instance constructor
    public RuleBasedJoinOrderPicker(SessionLocal session, TableFilter[] filters) {
        this.currentSession = session;
        this.filters = filters;
    }

    public TableFilter[] bestOrder() {
        Map<TableFilter, List<TableFilter>> connectionGraph = buildConnectionGraph();
        Optional<TableFilter> initialFilter = selectInitialFilter();
        List<TableFilter> optimalOrder = new ArrayList<>();
        
        // traverse the graph to get to best join order
        initialFilter.ifPresent(filter -> 
            getOrder(filter, connectionGraph, optimalOrder, new HashSet<>()));
        
        return optimalOrder.toArray(new TableFilter[0]);
    }

    private Map<TableFilter, List<TableFilter>> buildConnectionGraph() {
        Map<TableFilter, List<TableFilter>> connectionGraph = new HashMap<>();
        Set<Expression> processedConditions = new HashSet<>();

        for (TableFilter filter : filters) {
            connectionGraph.put(filter, new ArrayList<>());
            Expression condition = filter.getFullCondition();
            
            // establish the connnections for each unique condition
            if (condition != null && !processedConditions.contains(condition)) {
                addConnections(condition, connectionGraph);
                processedConditions.add(condition);
            }
        }
        return connectionGraph;
    }

    private void addConnections(Expression expr, Map<TableFilter, List<TableFilter>> graph) {
        if (expr instanceof ConditionAndOr || expr instanceof ConditionAndOrN) {
            // process for AND/OR conditions
            for (int i = 0; i < expr.getSubexpressionCount(); i++) {
                addConnections(expr.getSubexpression(i), graph);
            }
        } else if (expr instanceof Comparison) {
            Expression leftExpr = expr.getSubexpression(0);
            Expression rightExpr = expr.getSubexpression(1);
    
            if (leftExpr instanceof ExpressionColumn && rightExpr instanceof ExpressionColumn) {
                TableFilter leftFilter = ((ExpressionColumn) leftExpr).getTableFilter();
                TableFilter rightFilter = ((ExpressionColumn) rightExpr).getTableFilter();
    
                if (isValidConnection(leftFilter, rightFilter)) {
                    createBidirectionalLink(leftFilter, rightFilter, graph);
                }
            }
        }
    }

    private boolean isValidConnection(TableFilter f1, TableFilter f2) {
        return f1 != null && f2 != null && f1 != f2;
    }

    private void createBidirectionalLink(TableFilter src, TableFilter dest, 
                                        Map<TableFilter, List<TableFilter>> graph) {
        addUniqueConnection(src, dest, graph);
        addUniqueConnection(dest, src, graph);
    }

    private void addUniqueConnection(TableFilter source, TableFilter target, 
                                    Map<TableFilter, List<TableFilter>> graph) {
        if (!graph.get(source).contains(target)) {
            graph.get(source).add(target);
        }
    }

    private Optional<TableFilter> selectInitialFilter() {
        return Arrays.stream(filters)
            .min(Comparator.comparingLong(f -> 
                f.getTable().getRowCountApproximation(currentSession)));
    }

    private void getOrder(TableFilter currentFilter, Map<TableFilter, List<TableFilter>> graph, List<TableFilter> order,
            Set<TableFilter> visited) {
        visited.add(currentFilter);
        order.add(currentFilter);

        // sortby row count
        graph.getOrDefault(currentFilter, Collections.emptyList())
            .stream()
            .sorted(Comparator.comparingLong(f -> 
                f.getTable().getRowCountApproximation(currentSession)))
            .forEach(neighbor -> {
                if (!visited.contains(neighbor)) {
                    getOrder(neighbor, graph, order, visited);
                }
            });
    }

}