package com.realestate.reports;

import com.realestate.reports.dto.CommissionReport;
import com.realestate.reports.dto.DealReport;
import com.realestate.reports.dto.LeadReport;
import com.realestate.reports.dto.PropertyReport;
import com.realestate.reports.dto.ReportOverview;
import com.realestate.reports.dto.SiteVisitReport;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    private static final MediaType CSV_MEDIA_TYPE = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/overview")
    public ReportOverview overview(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        validateRange(from, to);
        return reportService.overview(authentication.getName(), from, to);
    }

    @GetMapping("/leads")
    public LeadReport leads(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status
    ) {
        validateRange(from, to);
        return reportService.leads(authentication.getName(), from, to, status);
    }

    @GetMapping("/properties")
    public PropertyReport properties(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status
    ) {
        validateRange(from, to);
        return reportService.properties(authentication.getName(), from, to, status);
    }

    @GetMapping("/site-visits")
    public SiteVisitReport siteVisits(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status
    ) {
        validateRange(from, to);
        return reportService.siteVisits(authentication.getName(), from, to, status);
    }

    @GetMapping("/deals")
    public DealReport deals(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status
    ) {
        validateRange(from, to);
        return reportService.deals(authentication.getName(), from, to, status);
    }

    @GetMapping("/commissions")
    public CommissionReport commissions(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        validateRange(from, to);
        return reportService.commissions(authentication.getName(), from, to);
    }

    @GetMapping(value = "/leads/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportLeads(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status
    ) {
        LeadReport report = leads(authentication, from, to, status);
        String csv = ReportCsv.create(
                List.of("Lead ID", "Name", "Source", "Status", "Interested Property", "Created At"),
                report.leads().stream().map(row -> List.of(
                        text(row.id()), text(row.name()), text(row.source()), text(row.status()),
                        text(row.interestedProperty()), text(row.createdAt())
                )).toList()
        );
        return csvAttachment("leads-report.csv", csv);
    }

    @GetMapping(value = "/properties/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportProperties(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status
    ) {
        PropertyReport report = properties(authentication, from, to, status);
        String csv = ReportCsv.create(
                List.of("Property ID", "Title", "Type", "Listing Type", "Status", "Available", "Price", "City", "Created At"),
                report.properties().stream().map(row -> List.of(
                        text(row.id()), text(row.title()), text(row.propertyType()), text(row.listingType()),
                        text(row.status()), text(row.available()), text(row.price()), text(row.city()), text(row.createdAt())
                )).toList()
        );
        return csvAttachment("properties-report.csv", csv);
    }

    @GetMapping(value = "/site-visits/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportSiteVisits(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status
    ) {
        SiteVisitReport report = siteVisits(authentication, from, to, status);
        String csv = ReportCsv.create(
                List.of("Visit ID", "Scheduled At", "Status", "Property ID", "Property", "Lead ID", "Lead", "Notes"),
                report.visits().stream().map(row -> List.of(
                        text(row.id()), text(row.scheduledAt()), text(row.status()), text(row.propertyId()),
                        text(row.propertyTitle()), text(row.leadId()), text(row.leadName()), text(row.notes())
                )).toList()
        );
        return csvAttachment("site-visits-report.csv", csv);
    }

    @GetMapping(value = "/deals/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportDeals(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status
    ) {
        DealReport report = deals(authentication, from, to, status);
        String csv = ReportCsv.create(
                List.of("Deal ID", "Lead", "Property", "Deal Value", "Stage", "Status", "Created At"),
                report.deals().stream().map(row -> List.of(
                        text(row.id()), text(row.leadName()), text(row.propertyTitle()), text(row.dealValue()),
                        text(row.stage()), text(row.status()), text(row.createdAt())
                )).toList()
        );
        return csvAttachment("deals-report.csv", csv);
    }

    @GetMapping(value = "/commissions/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCommissions(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        CommissionReport report = commissions(authentication, from, to);
        String csv = ReportCsv.create(
                List.of("Deal ID", "Lead", "Property", "Deal Value", "Commission Rate", "Commission Amount", "Date"),
                report.commissions().stream().map(row -> List.of(
                        text(row.dealId()), text(row.leadName()), text(row.propertyTitle()), text(row.dealValue()),
                        text(row.commissionRate()), text(row.commissionAmount()), text(row.date())
                )).toList()
        );
        return csvAttachment("commissions-report.csv", csv);
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' date must be on or before 'to'");
        }
    }

    private static ResponseEntity<byte[]> csvAttachment(String filename, String csv) {
        return ResponseEntity.ok()
                .contentType(CSV_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
