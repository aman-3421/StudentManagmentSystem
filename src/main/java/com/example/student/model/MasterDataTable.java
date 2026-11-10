package com.example.student.model;

import java.util.List;

public record MasterDataTable(List<String> columns, List<Object[]> rows) {
}
