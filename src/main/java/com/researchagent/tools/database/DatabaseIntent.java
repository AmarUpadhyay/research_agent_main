package com.researchagent.tools.database;

import java.util.ArrayList;
import java.util.List;

public class DatabaseIntent {

    private String entity;
    private DatabaseOperation operation;
    private List<String> columns = new ArrayList<>();
    private List<FilterCondition> filters = new ArrayList<>();
    private Integer limit;

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public DatabaseOperation getOperation() {
        return operation;
    }

    public void setOperation(DatabaseOperation operation) {
        this.operation = operation;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns == null ? new ArrayList<>() : columns;
    }

    public List<FilterCondition> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterCondition> filters) {
        this.filters = filters == null ? new ArrayList<>() : filters;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}