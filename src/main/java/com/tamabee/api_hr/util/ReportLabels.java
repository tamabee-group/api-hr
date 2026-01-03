package com.tamabee.api_hr.util;

import java.util.Map;

/**
 * Labels cho báo cáo theo ngôn ngữ
 */
public final class ReportLabels {

    private ReportLabels() {
    }

    // ==================== Attendance Summary ====================
    public static final Map<String, String[]> ATTENDANCE_HEADERS = Map.of(
            "vi", new String[] { "Mã NV", "Tên NV", "Ngày làm việc", "Có mặt", "Vắng mặt",
                    "Đi muộn (lần)", "Đi muộn (phút)", "Về sớm (lần)", "Về sớm (phút)",
                    "Tổng phút làm việc", "TB phút/ngày" },
            "ja", new String[] { "社員コード", "氏名", "勤務日数", "出勤", "欠勤",
                    "遅刻(回)", "遅刻(分)", "早退(回)", "早退(分)",
                    "総勤務時間(分)", "平均(分/日)" },
            "en", new String[] { "Code", "Name", "Work Days", "Present", "Absent",
                    "Late (times)", "Late (min)", "Early (times)", "Early (min)",
                    "Total Min", "Avg Min/Day" });

    public static final Map<String, String> ATTENDANCE_TITLE = Map.of(
            "vi", "Báo Cáo Tổng Hợp Chấm Công",
            "ja", "勤怠サマリーレポート",
            "en", "Attendance Summary Report");

    public static final Map<String, String> PERIOD_LABEL = Map.of(
            "vi", "Kỳ báo cáo",
            "ja", "期間",
            "en", "Period");

    public static final Map<String, String> TOTAL_EMPLOYEES_LABEL = Map.of(
            "vi", "Tổng nhân viên",
            "ja", "総従業員数",
            "en", "Total Employees");

    public static final Map<String, String> ATTENDANCE_RATE_LABEL = Map.of(
            "vi", "Tỷ lệ có mặt",
            "ja", "出勤率",
            "en", "Attendance Rate");

    // ==================== Overtime ====================
    public static final Map<String, String[]> OVERTIME_HEADERS = Map.of(
            "vi", new String[] { "Mã NV", "Tên NV", "OT thường (phút)", "OT đêm (phút)",
                    "OT lễ (phút)", "OT cuối tuần (phút)", "Tổng OT (phút)" },
            "ja", new String[] { "社員コード", "氏名", "通常残業(分)", "深夜残業(分)",
                    "祝日残業(分)", "週末残業(分)", "総残業(分)" },
            "en", new String[] { "Code", "Name", "Regular OT", "Night OT",
                    "Holiday OT", "Weekend OT", "Total OT" });

    public static final Map<String, String> OVERTIME_TITLE = Map.of(
            "vi", "Báo Cáo Làm Thêm Giờ",
            "ja", "残業レポート",
            "en", "Overtime Report");

    public static final Map<String, String> EMPLOYEES_WITH_OT_LABEL = Map.of(
            "vi", "Nhân viên có OT",
            "ja", "残業者数",
            "en", "Employees with OT");

    public static final Map<String, String> TOTAL_OT_LABEL = Map.of(
            "vi", "Tổng OT",
            "ja", "総残業時間",
            "en", "Total OT");

    // ==================== Break Compliance ====================
    public static final Map<String, String[]> BREAK_HEADERS = Map.of(
            "vi", new String[] { "Mã NV", "Tên NV", "Số lần nghỉ", "Tổng phút nghỉ",
                    "TB phút/ngày", "Tuân thủ", "Không tuân thủ", "Tỷ lệ tuân thủ (%)" },
            "ja", new String[] { "社員コード", "氏名", "休憩回数", "総休憩時間(分)",
                    "平均(分/日)", "遵守", "違反", "遵守率(%)" },
            "en", new String[] { "Code", "Name", "Break Count", "Total Min",
                    "Avg/Day", "Compliant", "Non-Compliant", "Rate %" });

    public static final Map<String, String> BREAK_TITLE = Map.of(
            "vi", "Báo Cáo Tuân Thủ Nghỉ Giải Lao",
            "ja", "休憩コンプライアンスレポート",
            "en", "Break Compliance Report");

    public static final Map<String, String> COMPLIANCE_RATE_LABEL = Map.of(
            "vi", "Tỷ lệ tuân thủ",
            "ja", "遵守率",
            "en", "Compliance Rate");

    // ==================== Payroll Summary ====================
    public static final Map<String, String[]> PAYROLL_HEADERS = Map.of(
            "vi", new String[] { "Mã NV", "Tên NV", "Loại lương", "Lương cơ bản", "Lương tính",
                    "OT", "Phụ cấp", "Khấu trừ", "Lương gộp", "Lương thực nhận" },
            "ja", new String[] { "社員コード", "氏名", "給与タイプ", "基本給", "計算給与",
                    "残業代", "手当", "控除", "総支給額", "手取り" },
            "en", new String[] { "Code", "Name", "Type", "Base", "Calculated",
                    "OT", "Allowance", "Deduction", "Gross", "Net" });

    public static final Map<String, String> PAYROLL_TITLE = Map.of(
            "vi", "Báo Cáo Tổng Hợp Lương",
            "ja", "給与サマリーレポート",
            "en", "Payroll Summary Report");

    public static final Map<String, String> TOTAL_GROSS_LABEL = Map.of(
            "vi", "Tổng lương gộp",
            "ja", "総支給額合計",
            "en", "Total Gross");

    public static final Map<String, String> TOTAL_NET_LABEL = Map.of(
            "vi", "Tổng lương thực nhận",
            "ja", "手取り合計",
            "en", "Total Net");

    // ==================== Shift Utilization ====================
    public static final Map<String, String[]> SHIFT_HEADERS = Map.of(
            "vi", new String[] { "Tên ca", "Giờ bắt đầu", "Giờ kết thúc", "Tổng phân công",
                    "Hoàn thành", "Hủy", "Đổi ca", "Tỷ lệ sử dụng (%)", "Tỷ lệ hoàn thành (%)" },
            "ja", new String[] { "シフト名", "開始時間", "終了時間", "総割当",
                    "完了", "キャンセル", "交換", "利用率(%)", "完了率(%)" },
            "en", new String[] { "Shift", "Start", "End", "Total",
                    "Completed", "Cancelled", "Swapped", "Util %", "Comp %" });

    public static final Map<String, String> SHIFT_TITLE = Map.of(
            "vi", "Báo Cáo Sử Dụng Ca Làm Việc",
            "ja", "シフト利用レポート",
            "en", "Shift Utilization Report");

    public static final Map<String, String> TOTAL_ASSIGNMENTS_LABEL = Map.of(
            "vi", "Tổng phân công",
            "ja", "総割当数",
            "en", "Total Assignments");

    public static final Map<String, String> COMPLETION_RATE_LABEL = Map.of(
            "vi", "Tỷ lệ hoàn thành",
            "ja", "完了率",
            "en", "Completion Rate");

    public static final Map<String, String> SWAP_REQUESTS_LABEL = Map.of(
            "vi", "Yêu cầu đổi ca",
            "ja", "シフト交換申請",
            "en", "Swap Requests");

    public static final Map<String, String> APPROVAL_RATE_LABEL = Map.of(
            "vi", "Tỷ lệ duyệt",
            "ja", "承認率",
            "en", "Approval Rate");

    // ==================== Cost Analysis ====================
    public static final Map<String, String[]> COST_HEADERS = Map.of(
            "vi", new String[] { "Loại", "Số NV", "Tổng chi phí", "Lương cơ bản", "OT", "Phụ cấp", "Tỷ lệ (%)" },
            "ja", new String[] { "タイプ", "従業員数", "総コスト", "基本給", "残業代", "手当", "割合(%)" },
            "en", new String[] { "Type", "Employees", "Total", "Base", "OT", "Allowance", "%" });

    public static final Map<String, String> COST_TITLE = Map.of(
            "vi", "Báo Cáo Phân Tích Chi Phí",
            "ja", "コスト分析レポート",
            "en", "Cost Analysis Report");

    public static final Map<String, String> TOTAL_LABOR_COST_LABEL = Map.of(
            "vi", "Tổng chi phí nhân sự",
            "ja", "総人件費",
            "en", "Total Labor Cost");

    public static final Map<String, String> BY_SALARY_TYPE_LABEL = Map.of(
            "vi", "Theo loại lương",
            "ja", "給与タイプ別",
            "en", "By Salary Type");

    public static final Map<String, String> BY_CONTRACT_TYPE_LABEL = Map.of(
            "vi", "Theo loại hợp đồng",
            "ja", "契約タイプ別",
            "en", "By Contract Type");

    // ==================== Helper Methods ====================
    public static String get(Map<String, String> labels, String language) {
        return labels.getOrDefault(language, labels.get("vi"));
    }

    public static String[] getHeaders(Map<String, String[]> headers, String language) {
        return headers.getOrDefault(language, headers.get("vi"));
    }
}
