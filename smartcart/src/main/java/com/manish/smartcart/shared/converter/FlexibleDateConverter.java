package com.manish.smartcart.shared.converter;

import com.manish.smartcart.shared.exception.BusinessLogicException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Accepts multiple human-friendly date formats in API query params.
 * Registered globally via WebConfig → applies to ALL LocalDate @RequestParams in the app.
 * <p>
 * SUPPORTED:
 *   2026-08-12  → ISO 8601 (primary — unambiguous, recommended)
 *   20260812    → Compact  (for Postman/curl convenience)
 * <p>
 * WHY NOT dd-MM-yyyy: "12/08" = Aug 12 in India, Dec 8 in the US. Ambiguous → real bugs.
 */
@Component
public class FlexibleDateConverter implements Converter<String, LocalDate> {

    // Tried in order — first match wins
    private static final List<DateTimeFormatter> FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"), // 2026-08-12
            DateTimeFormatter.ofPattern("yyyyMMdd")    // 20260812
    );

    @Override
    public LocalDate convert(String source) {
        if (source == null || source.isBlank()) return null;

        for (DateTimeFormatter fmt : FORMATS) {
            try {
                return LocalDate.parse(source.trim(), fmt);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }

        // None matched — give caller a clear, actionable error
        throw new BusinessLogicException(
                "Invalid date: '" + source + "'. Use yyyy-MM-dd (e.g. 2026-08-12) or yyyyMMdd (e.g. 20260812)."
        );
    }
}
