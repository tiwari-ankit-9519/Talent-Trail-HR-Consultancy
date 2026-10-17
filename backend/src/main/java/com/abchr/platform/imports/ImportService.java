package com.abchr.platform.imports;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.imports.ImportDtos.ImportReport;
import com.abchr.platform.imports.ImportDtos.ImportRequest;
import com.abchr.platform.shared.security.Permissions;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ImportService {

    private final Map<String, ImportHandler> handlers;
    private final AuditService auditService;

    ImportService(List<ImportHandler> handlers, AuditService auditService) {
        this.handlers = handlers.stream().collect(Collectors.toMap(ImportHandler::type, Function.identity()));
        this.auditService = auditService;
    }

    public ImportReport preview(String type, ImportRequest request) {
        return handler(type).process(request.rows(), false);
    }

    public ImportReport commit(String type, ImportRequest request) {
        ImportHandler handler = handler(type);
        ImportReport report = handler.process(request.rows(), true);
        auditService.record(AuditActions.IMPORT_COMMITTED, AuditActions.ENTITY_IMPORT, type,
                "Imported " + report.imported() + " of " + report.total() + " " + type + " ("
                        + report.duplicates() + " duplicates, " + report.invalid() + " invalid)", null);
        return report;
    }

    private ImportHandler handler(String type) {
        ImportHandler handler = handlers.get(type);
        if (handler == null) {
            throw ApiException.notFound(ErrorCodes.IMPORT_TYPE_UNKNOWN, "Unknown import type");
        }
        for (String permission : handler.requiredPermissions()) {
            if (!Permissions.hasAny(permission)) {
                throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "You do not have permission to do this");
            }
        }
        return handler;
    }
}
