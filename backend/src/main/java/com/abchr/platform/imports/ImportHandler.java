package com.abchr.platform.imports;

import com.abchr.platform.imports.ImportDtos.ImportReport;
import java.util.List;
import java.util.Map;

interface ImportHandler {

    String type();

    String[] requiredPermissions();

    ImportReport process(List<Map<String, String>> rows, boolean commit);
}
