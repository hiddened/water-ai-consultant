package com.waterai.consultant.structured;

import java.util.List;
import java.util.Set;

record CrudResource(
        String tableName,
        List<String> writableColumns,
        List<String> selectColumns,
        List<String> keywordColumns,
        Set<String> uuidColumns,
        Set<String> jsonColumns,
        Set<String> timestampColumns,
        boolean projectScoped,
        boolean moduleScoped
) {
}

