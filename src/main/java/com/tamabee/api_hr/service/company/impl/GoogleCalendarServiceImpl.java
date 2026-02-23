package com.tamabee.api_hr.service.company.impl;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.dto.response.leave.HolidayResponse;
import com.tamabee.api_hr.entity.leave.HolidayEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.HolidayType;
import com.tamabee.api_hr.exception.InternalServerException;
import com.tamabee.api_hr.mapper.company.HolidayMapper;
import com.tamabee.api_hr.repository.leave.HolidayRepository;
import com.tamabee.api_hr.service.company.interfaces.IGoogleCalendarService;

import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation tích hợp Google Calendar API để sync ngày lễ quốc gia.
 * Hỗ trợ lấy ngày lễ cho Việt Nam (vi) và Nhật Bản (ja).
 */
@Slf4j
@Service
public class GoogleCalendarServiceImpl implements IGoogleCalendarService {

    private final RestTemplate restTemplate;
    private final HolidayRepository holidayRepository;
    private final HolidayMapper holidayMapper;
    private final JdbcTemplate masterJdbcTemplate;

    @Value("${google.calendar.api-key:}")
    private String apiKey;

    public GoogleCalendarServiceImpl(
            RestTemplate restTemplate,
            HolidayRepository holidayRepository,
            HolidayMapper holidayMapper,
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate) {
        this.restTemplate = restTemplate;
        this.holidayRepository = holidayRepository;
        this.holidayMapper = holidayMapper;
        this.masterJdbcTemplate = masterJdbcTemplate;
    }

    /** Calendar IDs theo region */
    private static final Map<String, String> CALENDAR_IDS = Map.of(
            "vi", "vi.vietnamese#holiday@group.v.calendar.google.com",
            "ja", "ja.japanese#holiday@group.v.calendar.google.com"
    );

    @Override
    @Transactional
    public List<HolidayResponse> syncHolidays(Integer year) {
        // Validate API key đã được cấu hình
        validateApiKey();

        // Tự động lấy region từ company trong master DB
        String region = resolveCompanyLocale();

        // Lấy calendar ID theo region
        String calendarId = getCalendarId(region);

        // Gọi Google Calendar API
        JsonNode eventsNode = fetchEventsFromGoogle(calendarId, year);

        // Parse và lưu holidays
        List<HolidayEntity> syncedHolidays = parseAndSaveHolidays(eventsNode);

        log.info("Đã sync {} ngày lễ quốc gia cho region={}, year={}", syncedHolidays.size(), region, year);

        return syncedHolidays.stream()
                .map(holidayMapper::toResponse)
                .toList();
    }

    // ==================== Private Helper Methods ====================

    /**
     * Validate API key đã được cấu hình
     */
    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new InternalServerException(
                    "Google Calendar API key chưa được cấu hình trong application properties",
                    ErrorCode.GOOGLE_CALENDAR_NOT_CONFIGURED);
        }
    }

    /**
     * Lấy region của company từ master DB dựa trên tenant domain hiện tại.
     * Normalize về dạng "vi" hoặc "ja".
     * Hỗ trợ cả region code (vi, ja) và timezone (Asia/Tokyo, Asia/Ho_Chi_Minh).
     */
    private String resolveCompanyLocale() {
        String tenantDomain = TenantContext.getCurrentTenant();
        if (tenantDomain == null) {
            return "ja"; // default khi không có tenant context
        }

        String sql = "SELECT region FROM companies WHERE tenant_domain = ? AND deleted = false";
        try {
            String rawRegion = masterJdbcTemplate.queryForObject(sql, String.class, tenantDomain);
            if (rawRegion == null) {
                return "ja";
            }
            return normalizeLocale(rawRegion);
        } catch (Exception e) {
            log.warn("Không thể lấy region của company tenant={}: {}", tenantDomain, e.getMessage());
            return "ja";
        }
    }

    /**
     * Normalize region value về dạng "vi" hoặc "ja".
     * Hỗ trợ: region code (vi, ja, vi_VN, ja_JP) và timezone (Asia/Tokyo, Asia/Ho_Chi_Minh).
     */
    private String normalizeLocale(String rawLocale) {
        String normalized = rawLocale.toLowerCase();
        if (normalized.startsWith("vi") || normalized.contains("ho_chi_minh")) {
            return "vi";
        }
        if (normalized.startsWith("ja") || normalized.contains("tokyo")) {
            return "ja";
        }
        return "ja"; // default
    }

    /**
     * Lấy calendar ID theo region
     */
    private String getCalendarId(String region) {
        String calendarId = CALENDAR_IDS.get(region);
        if (calendarId == null) {
            throw new InternalServerException(
                    "Region không được hỗ trợ: " + region + ". Chỉ hỗ trợ: vi, ja",
                    ErrorCode.GOOGLE_CALENDAR_ERROR);
        }
        return calendarId;
    }

    /**
     * Gọi Google Calendar API để lấy events
     */
    private JsonNode fetchEventsFromGoogle(String calendarId, Integer year) {
        try {
            // Xây dựng URL - encode calendarId vì chứa ký tự đặc biệt (#, @)
            String timeMin = year + "-01-01T00:00:00Z";
            String timeMax = year + "-12-31T23:59:59Z";
            String encodedCalendarId = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);

            // Dùng URI trực tiếp để tránh RestTemplate double-encode
            String urlStr = "https://www.googleapis.com/calendar/v3/calendars/" + encodedCalendarId + "/events"
                    + "?key=" + apiKey
                    + "&timeMin=" + timeMin
                    + "&timeMax=" + timeMax
                    + "&singleEvents=true"
                    + "&orderBy=startTime"
                    + "&maxResults=100";

            URI uri = URI.create(urlStr);

            log.debug("Gọi Google Calendar API: calendarId={}, year={}", calendarId, year);

            ResponseEntity<JsonNode> response = restTemplate.getForEntity(uri, JsonNode.class);

            if (response.getBody() == null) {
                throw new InternalServerException(
                        "Google Calendar API trả về response rỗng",
                        ErrorCode.GOOGLE_CALENDAR_ERROR);
            }

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Lỗi khi gọi Google Calendar API: {}", e.getMessage(), e);
            throw new InternalServerException(
                    "Không thể kết nối đến Google Calendar API: " + e.getMessage(),
                    ErrorCode.GOOGLE_CALENDAR_ERROR,
                    e);
        }
    }

    /**
     * Parse events từ Google Calendar response và lưu vào database.
     * Xử lý duplicate bằng upsert (kiểm tra theo date).
     */
    private List<HolidayEntity> parseAndSaveHolidays(JsonNode responseBody) {
        List<HolidayEntity> syncedHolidays = new ArrayList<>();

        JsonNode items = responseBody.get("items");
        if (items == null || !items.isArray()) {
            log.warn("Google Calendar API không trả về items hoặc items rỗng");
            return syncedHolidays;
        }

        for (JsonNode item : items) {
            try {
                HolidayEntity holiday = parseAndUpsertHoliday(item);
                if (holiday != null) {
                    syncedHolidays.add(holiday);
                }
            } catch (Exception e) {
                // Log lỗi nhưng tiếp tục xử lý các events khác
                String summary = item.has("summary") ? item.get("summary").asText() : "unknown";
                log.warn("Không thể parse event: {}. Lỗi: {}", summary, e.getMessage());
            }
        }

        return syncedHolidays;
    }

    /**
     * Parse một event từ Google Calendar và upsert vào database.
     * Nếu ngày lễ đã tồn tại (cùng date) → cập nhật tên.
     * Nếu chưa tồn tại → tạo mới.
     */
    private HolidayEntity parseAndUpsertHoliday(JsonNode item) {
        // Lấy tên ngày lễ
        String name = item.has("summary") ? item.get("summary").asText() : null;
        if (name == null || name.isBlank()) {
            return null;
        }

        // Lấy ngày từ event (Google Calendar holiday dùng "date" thay vì "dateTime")
        LocalDate date = extractDate(item);
        if (date == null) {
            return null;
        }

        // Upsert: kiểm tra đã tồn tại chưa theo date
        Optional<HolidayEntity> existingOpt = holidayRepository.findByDateAndDeletedFalse(date);

        if (existingOpt.isPresent()) {
            // Đã tồn tại → cập nhật tên nếu khác
            HolidayEntity existing = existingOpt.get();
            if (!existing.getName().equals(name)) {
                existing.setName(name);
                existing = holidayRepository.save(existing);
                log.debug("Đã cập nhật ngày lễ: {} - {}", date, name);
            }
            return existing;
        } else {
            // Chưa tồn tại → tạo mới
            HolidayEntity newHoliday = new HolidayEntity();
            newHoliday.setDate(date);
            newHoliday.setName(name);
            newHoliday.setType(HolidayType.NATIONAL);
            newHoliday.setIsPaid(true);
            newHoliday.setDescription("Google Calendar");
            newHoliday = holidayRepository.save(newHoliday);
            log.debug("Đã tạo ngày lễ mới: {} - {}", date, name);
            return newHoliday;
        }
    }

    /**
     * Trích xuất ngày từ Google Calendar event.
     * Google Calendar holiday events dùng "start.date" (all-day event).
     */
    private LocalDate extractDate(JsonNode item) {
        JsonNode startNode = item.get("start");
        if (startNode == null) {
            return null;
        }

        // All-day events dùng "date" field (format: yyyy-MM-dd)
        if (startNode.has("date")) {
            String dateStr = startNode.get("date").asText();
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                log.warn("Không thể parse date: {}", dateStr);
                return null;
            }
        }

        // Fallback: dateTime field (format: yyyy-MM-ddTHH:mm:ssZ)
        if (startNode.has("dateTime")) {
            String dateTimeStr = startNode.get("dateTime").asText();
            try {
                return LocalDate.parse(dateTimeStr.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                log.warn("Không thể parse dateTime: {}", dateTimeStr);
                return null;
            }
        }

        return null;
    }
}
