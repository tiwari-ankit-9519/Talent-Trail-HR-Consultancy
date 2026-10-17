package com.abchr.platform.audit;

public final class AuditActions {

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String LOGOUT = "LOGOUT";
    public static final String TOKEN_REFRESHED = "TOKEN_REFRESHED";
    public static final String TOKEN_REUSE_DETECTED = "TOKEN_REUSE_DETECTED";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";

    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String USER_BOOTSTRAPPED = "USER_BOOTSTRAPPED";

    public static final String ROLE_CREATED = "ROLE_CREATED";
    public static final String ROLE_UPDATED = "ROLE_UPDATED";
    public static final String ROLE_DELETED = "ROLE_DELETED";

    public static final String CLIENT_CREATED = "CLIENT_CREATED";
    public static final String CLIENT_UPDATED = "CLIENT_UPDATED";
    public static final String CLIENT_TERMS_UPDATED = "CLIENT_TERMS_UPDATED";
    public static final String CLIENT_STATUS_CHANGED = "CLIENT_STATUS_CHANGED";
    public static final String CONTACT_ADDED = "CONTACT_ADDED";
    public static final String CONTACT_UPDATED = "CONTACT_UPDATED";
    public static final String CONTACT_REMOVED = "CONTACT_REMOVED";

    public static final String JOB_CREATED = "JOB_CREATED";
    public static final String JOB_UPDATED = "JOB_UPDATED";
    public static final String JOB_STATUS_CHANGED = "JOB_STATUS_CHANGED";
    public static final String CANDIDATE_CREATED = "CANDIDATE_CREATED";
    public static final String CANDIDATE_UPDATED = "CANDIDATE_UPDATED";
    public static final String CANDIDATE_STATUS_CHANGED = "CANDIDATE_STATUS_CHANGED";
    public static final String CANDIDATE_CONSENT_RECORDED = "CANDIDATE_CONSENT_RECORDED";
    public static final String CANDIDATE_RESUME_UPLOADED = "CANDIDATE_RESUME_UPLOADED";
    public static final String CANDIDATE_RESUME_DOWNLOADED = "CANDIDATE_RESUME_DOWNLOADED";
    public static final String CANDIDATE_LINKED_TO_JOB = "CANDIDATE_LINKED_TO_JOB";
    public static final String CANDIDATE_UNLINKED_FROM_JOB = "CANDIDATE_UNLINKED_FROM_JOB";
    public static final String PUBLIC_APPLICATION = "PUBLIC_APPLICATION";

    public static final String SUBMISSION_CREATED = "SUBMISSION_CREATED";
    public static final String SUBMISSION_EMAIL_SENT = "SUBMISSION_EMAIL_SENT";
    public static final String SUBMISSION_EMAIL_FAILED = "SUBMISSION_EMAIL_FAILED";
    public static final String SUBMISSION_WITHDRAWN = "SUBMISSION_WITHDRAWN";
    public static final String SUBMISSION_STATUS_CHANGED = "SUBMISSION_STATUS_CHANGED";
    public static final String INTERVIEW_SCHEDULED = "INTERVIEW_SCHEDULED";
    public static final String INTERVIEW_UPDATED = "INTERVIEW_UPDATED";
    public static final String INTERVIEW_STATUS_CHANGED = "INTERVIEW_STATUS_CHANGED";
    public static final String FEEDBACK_RECORDED = "FEEDBACK_RECORDED";

    public static final String OFFER_RECORDED = "OFFER_RECORDED";
    public static final String OFFER_UPDATED = "OFFER_UPDATED";
    public static final String OFFER_EMAIL = "OFFER_EMAIL";
    public static final String OFFER_ACCEPTED = "OFFER_ACCEPTED";
    public static final String OFFER_DECLINED = "OFFER_DECLINED";
    public static final String JOINING_RECORDED = "JOINING_RECORDED";
    public static final String NOT_JOINED_RECORDED = "NOT_JOINED_RECORDED";
    public static final String JOINING_EMAIL_UPLOADED = "JOINING_EMAIL_UPLOADED";
    public static final String JOINING_EMAIL_DOWNLOADED = "JOINING_EMAIL_DOWNLOADED";
    public static final String COMPANY_UPDATED = "COMPANY_UPDATED";
    public static final String INVOICE_CREATED = "INVOICE_CREATED";
    public static final String INVOICE_SENT = "INVOICE_SENT";
    public static final String INVOICE_SEND_FAILED = "INVOICE_SEND_FAILED";
    public static final String INVOICE_CANCELLED = "INVOICE_CANCELLED";
    public static final String PAYMENT_RECORDED = "PAYMENT_RECORDED";
    public static final String PAYMENT_REMOVED = "PAYMENT_REMOVED";
    public static final String EXIT_RECORDED = "EXIT_RECORDED";
    public static final String REPLACEMENT_LINKED = "REPLACEMENT_LINKED";
    public static final String REPLACEMENT_CLOSED = "REPLACEMENT_CLOSED";
    public static final String COMMISSION_CREATED = "COMMISSION_CREATED";
    public static final String COMMISSION_AMOUNT_SET = "COMMISSION_AMOUNT_SET";
    public static final String COMMISSION_PAID = "COMMISSION_PAID";
    public static final String COMMISSION_CANCELLED = "COMMISSION_CANCELLED";
    public static final String ENTITY_COMMISSION = "COMMISSION";
    public static final String ENTITY_COMPANY = "COMPANY";
    public static final String ENTITY_INVOICE = "INVOICE";
    public static final String ENTITY_PLACEMENT = "PLACEMENT";

    public static final String ENTITY_SUBMISSION = "SUBMISSION";
    public static final String ENTITY_INTERVIEW = "INTERVIEW";
    public static final String ENTITY_JOB = "JOB";
    public static final String ENTITY_CANDIDATE = "CANDIDATE";
    public static final String ENTITY_USER = "USER";
    public static final String ENTITY_ROLE = "ROLE";
    public static final String ENTITY_CLIENT = "CLIENT";
    public static final String ENTITY_CONTACT = "CLIENT_CONTACT";
    public static final String ENTITY_SESSION = "SESSION";
    public static final String ATTENDANCE_CHECK_IN = "ATTENDANCE_CHECK_IN";
    public static final String ATTENDANCE_CHECK_OUT = "ATTENDANCE_CHECK_OUT";
    public static final String ATTENDANCE_MARKED = "ATTENDANCE_MARKED";
    public static final String ATTENDANCE_REQUEST_CREATED = "ATTENDANCE_REQUEST_CREATED";
    public static final String ATTENDANCE_REQUEST_APPROVED = "ATTENDANCE_REQUEST_APPROVED";
    public static final String ATTENDANCE_REQUEST_REJECTED = "ATTENDANCE_REQUEST_REJECTED";
    public static final String ATTENDANCE_SETTINGS_UPDATED = "ATTENDANCE_SETTINGS_UPDATED";
    public static final String HOLIDAY_ADDED = "HOLIDAY_ADDED";
    public static final String HOLIDAY_REMOVED = "HOLIDAY_REMOVED";
    public static final String ENTITY_ATTENDANCE = "ATTENDANCE";
    public static final String ENTITY_ATTENDANCE_REQUEST = "ATTENDANCE_REQUEST";
    public static final String ENTITY_ATTENDANCE_SETTINGS = "ATTENDANCE_SETTINGS";
    public static final String ENTITY_HOLIDAY = "HOLIDAY";
    public static final String LEAVE_OPTIONAL_CLAIMED = "LEAVE_OPTIONAL_CLAIMED";
    public static final String LEAVE_COMP_OFF_USED = "LEAVE_COMP_OFF_USED";
    public static final String LEAVE_CANCELLED = "LEAVE_CANCELLED";
    public static final String COMP_OFF_EARNED = "COMP_OFF_EARNED";
    public static final String COMP_OFF_GRANTED = "COMP_OFF_GRANTED";
    public static final String ENTITY_LEAVE = "ENTITY_LEAVE";
    public static final String ENTITY_COMP_OFF = "ENTITY_COMP_OFF";
    public static final String LEAVE_APPROVED = "LEAVE_APPROVED";
    public static final String LEAVE_REJECTED = "LEAVE_REJECTED";
    public static final String ENTITY_REQUEST = "REQUEST";

    private AuditActions() {
    }
}
