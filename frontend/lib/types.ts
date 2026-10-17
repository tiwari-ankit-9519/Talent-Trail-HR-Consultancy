export type UserProfile = {
  id: string;
  email: string;
  fullName: string;
  roles: string[];
  permissions: string[];
  mustChangePassword: boolean;
};

export type Session = {
  accessToken: string;
  expiresInSeconds: number;
  user: UserProfile;
};

export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

export type RoleRef = { id: string; name: string };

export type UserRow = {
  id: string;
  email: string;
  fullName: string;
  status: "ACTIVE" | "INACTIVE";
  mustChangePassword: boolean;
  locked: boolean;
  lastLoginAt: string | null;
  roles: RoleRef[];
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type RoleRow = {
  id: string;
  name: string;
  description: string | null;
  systemRole: boolean;
  permissions: string[];
  userCount: number;
  version: number;
};

export type PermissionRow = { code: string; description: string };

export type ClientSummary = {
  id: string;
  name: string;
  industry: string | null;
  city: string | null;
  state: string | null;
  status: "ACTIVE" | "INACTIVE";
  updatedAt: string;
};

export type FeeType = "MONTHS_OF_SALARY" | "CTC_PERCENT";

export type ClientTerms = {
  feeType: FeeType;
  feeValue: number;
  gstApplicable: boolean;
  gstRate: number;
  tdsApplicable: boolean;
  tdsRate: number;
  paymentTermsDays: number;
  replacementPeriodDays: number;
};

export type ClientContact = {
  id: string;
  name: string;
  email: string;
  phone: string | null;
  designation: string | null;
  primaryContact: boolean;
};

export type ClientDetail = {
  id: string;
  name: string;
  legalName: string | null;
  industry: string | null;
  website: string | null;
  gstin: string | null;
  pan: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  status: "ACTIVE" | "INACTIVE";
  notes: string | null;
  terms: ClientTerms | null;
  contacts: ClientContact[];
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type AuditRow = {
  id: string;
  occurredAt: string;
  actorEmail: string | null;
  action: string;
  entityType: string | null;
  entityId: string | null;
  summary: string;
  details: string | null;
  outcome: "SUCCESS" | "FAILURE";
  ipAddress: string | null;
  requestId: string | null;
};

export type JobStatus = "DRAFT" | "OPEN" | "ON_HOLD" | "CLOSED";

export type JobSummary = {
  id: string;
  code: string;
  title: string;
  clientId: string;
  clientName: string | null;
  location: string | null;
  openings: number;
  status: JobStatus;
  published: boolean;
  closingDate: string | null;
  applicantCount: number;
  updatedAt: string;
};

export type JobDetail = {
  id: string;
  code: string;
  clientId: string;
  clientName: string | null;
  title: string;
  description: string | null;
  location: string | null;
  skills: string | null;
  minExperienceYears: number | null;
  maxExperienceYears: number | null;
  minCtc: number | null;
  maxCtc: number | null;
  openings: number;
  status: JobStatus;
  published: boolean;
  closingDate: string | null;
  applicantCount: number;
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type PublicJob = {
  code: string;
  title: string;
  description: string | null;
  location: string | null;
  skills: string | null;
  minExperienceYears: number | null;
  maxExperienceYears: number | null;
  openings: number;
  closingDate: string | null;
};

export type CandidateSource = "LINKEDIN" | "NAUKRI" | "APPLY_LINK" | "REFERRAL" | "OTHER";
export type CandidateStatus = "NEW" | "SCREENING" | "AVAILABLE" | "NOT_INTERESTED" | "PLACED" | "BLOCKED";
export type ConsentStatus = "NOT_ASKED" | "GRANTED" | "DENIED" | "WITHDRAWN";
export type ConsentMethod = "CALL" | "EMAIL" | "APPLY_FORM";

export type CandidateSummary = {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  currentTitle: string | null;
  currentCompany: string | null;
  totalExperienceYears: number | null;
  source: CandidateSource;
  status: CandidateStatus;
  consentStatus: ConsentStatus;
  hasResume: boolean;
  createdAt: string;
};

export type ConsentEvent = {
  status: ConsentStatus;
  method: ConsentMethod;
  note: string | null;
  recordedBy: string;
  recordedAt: string;
};

export type ApplicationRow = {
  id: string;
  jobId: string;
  jobCode: string | null;
  jobTitle: string | null;
  clientName: string | null;
  source: CandidateSource;
  appliedAt: string;
};

export type CandidateDetail = {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  currentCompany: string | null;
  currentTitle: string | null;
  totalExperienceYears: number | null;
  currentCtc: number | null;
  expectedCtc: number | null;
  noticePeriodDays: number | null;
  currentLocation: string | null;
  preferredLocation: string | null;
  skills: string | null;
  source: CandidateSource;
  status: CandidateStatus;
  notes: string | null;
  hasResume: boolean;
  resumeFileName: string | null;
  resumeSize: number | null;
  consentStatus: ConsentStatus;
  consentMethod: ConsentMethod | null;
  consentRecordedAt: string | null;
  consentHistory: ConsentEvent[];
  applications: ApplicationRow[];
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type SubmissionStatus =
  "SUBMITTED" | "SHORTLISTED" | "INTERVIEWING" | "SELECTED" | "REJECTED" | "ON_HOLD" | "WITHDRAWN";
export type FeedbackDecision = "SHORTLISTED" | "SELECTED" | "REJECTED" | "ON_HOLD";
export type InterviewMode = "PHONE" | "VIDEO" | "IN_PERSON";
export type InterviewStatus = "SCHEDULED" | "COMPLETED" | "CANCELLED" | "NO_SHOW";
export type EmailStatus = "PENDING" | "QUEUED" | "SENT" | "FAILED" | "NOT_CONFIGURED" | "SKIPPED";

export type ContactOption = {
  id: string;
  name: string;
  email: string;
  designation: string | null;
  primaryContact: boolean;
};

export type SubmissionSummary = {
  id: string;
  jobId: string;
  jobCode: string | null;
  jobTitle: string | null;
  clientId: string;
  clientName: string | null;
  candidateId: string;
  candidateName: string | null;
  status: SubmissionStatus;
  emailStatus: EmailStatus;
  submittedAt: string;
};

export type InterviewRow = {
  id: string;
  roundNumber: number;
  scheduledAt: string;
  durationMinutes: number;
  mode: InterviewMode;
  location: string | null;
  interviewerName: string | null;
  status: InterviewStatus;
  notes: string | null;
  inviteStatus: EmailStatus;
  inviteSentAt: string | null;
  inviteError: string | null;
  version: number;
};

export type FeedbackRow = {
  id: string;
  interviewId: string | null;
  decision: FeedbackDecision;
  comments: string | null;
  receivedOn: string;
  recordedBy: string | null;
  recordedAt: string;
};

export type SubmissionDetail = {
  id: string;
  jobId: string;
  jobCode: string;
  jobTitle: string;
  clientId: string;
  clientName: string;
  candidateId: string;
  candidateName: string;
  candidateEmail: string;
  candidatePhone: string | null;
  status: SubmissionStatus;
  recipients: string[];
  note: string | null;
  emailStatus: EmailStatus;
  emailSentAt: string | null;
  emailError: string | null;
  submittedAt: string;
  submittedBy: string | null;
  interviews: InterviewRow[];
  feedback: FeedbackRow[];
  version: number;
};

export type InterviewListItem = {
  id: string;
  submissionId: string;
  roundNumber: number;
  scheduledAt: string;
  durationMinutes: number;
  mode: InterviewMode;
  status: InterviewStatus;
  candidateName: string | null;
  jobCode: string | null;
  jobTitle: string | null;
  clientName: string | null;
};

export type PlacementStatus = "OFFERED" | "OFFER_ACCEPTED" | "OFFER_DECLINED" | "JOINED" | "NOT_JOINED";

export type PlacementSummary = {
  id: string;
  submissionId: string;
  jobId: string;
  jobCode: string | null;
  jobTitle: string | null;
  clientId: string;
  clientName: string | null;
  candidateId: string;
  candidateName: string | null;
  status: PlacementStatus;
  offeredCtc: number;
  expectedJoiningDate: string;
  joiningDate: string | null;
  joiningEmailAttached: boolean;
};

export type PlacementDetail = {
  id: string;
  submissionId: string;
  jobId: string;
  jobCode: string | null;
  jobTitle: string | null;
  clientId: string;
  clientName: string | null;
  candidateId: string;
  candidateName: string | null;
  status: PlacementStatus;
  offeredCtc: number;
  offerDate: string;
  expectedJoiningDate: string;
  acceptedOn: string | null;
  closeReason: string | null;
  joiningDate: string | null;
  joiningEmailAttached: boolean;
  joiningEmailFileName: string | null;
  joiningEmailSize: number | null;
  notes: string | null;
  offerEmailStatus: EmailStatus;
  offerEmailSentAt: string | null;
  offerEmailError: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type InvoiceStatus = "ISSUED" | "PARTIALLY_PAID" | "PAID" | "CANCELLED";

export type PaymentMode = "BANK_TRANSFER" | "UPI" | "CHEQUE" | "CASH" | "OTHER";

export type InvoiceSummary = {
  id: string;
  number: string;
  status: InvoiceStatus;
  overdue: boolean;
  clientId: string;
  clientName: string;
  candidateName: string;
  jobCode: string;
  invoiceDate: string;
  dueDate: string;
  totalAmount: number;
  settledAmount: number;
  balance: number;
  emailStatus: EmailStatus;
};

export type InvoicePaymentView = {
  id: string;
  paidOn: string;
  amountReceived: number;
  tdsDeducted: number;
  mode: PaymentMode;
  reference: string | null;
  note: string | null;
  recordedBy: string | null;
};

export type InvoiceContact = {
  name: string;
  email: string;
  primary: boolean;
};

export type InvoiceDetail = {
  id: string;
  number: string;
  status: InvoiceStatus;
  overdue: boolean;
  placementId: string;
  clientId: string;
  clientName: string;
  jobId: string;
  jobCode: string;
  jobTitle: string;
  candidateId: string;
  candidateName: string;
  invoiceDate: string;
  dueDate: string;
  joiningDate: string;
  description: string;
  sacCode: string;
  offeredCtc: number;
  feeType: FeeType;
  feeValue: number;
  baseAmount: number;
  gstRate: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  totalAmount: number;
  tdsRate: number;
  tdsAmount: number;
  netReceivable: number;
  settledAmount: number;
  balance: number;
  cancelReason: string | null;
  cancelledAt: string | null;
  emailStatus: EmailStatus;
  emailSentAt: string | null;
  emailError: string | null;
  sentTo: string | null;
  payments: InvoicePaymentView[];
  contacts: InvoiceContact[];
  version: number;
};

export type CompanyProfile = {
  legalName: string;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  gstin: string | null;
  pan: string | null;
  email: string | null;
  phone: string | null;
  sacCode: string;
  invoicePrefix: string;
  bankAccountName: string | null;
  bankName: string | null;
  bankAccountNumber: string | null;
  bankIfsc: string | null;
  bankBranch: string | null;
  invoiceFooter: string | null;
  version: number;
};

export type ReplacementStatus = "OPEN" | "FILLED" | "CLOSED" | "NOT_ELIGIBLE";

export type ReplacementView = {
  id: string;
  placementId: string;
  clientId: string;
  clientName: string;
  jobId: string;
  jobCode: string | null;
  jobTitle: string | null;
  candidateId: string;
  candidateName: string;
  joiningDate: string;
  exitDate: string;
  exitReason: string;
  periodDays: number;
  freeUntil: string;
  withinPeriod: boolean;
  status: ReplacementStatus;
  replacementPlacementId: string | null;
  replacementCandidateName: string | null;
  closeNote: string | null;
  version: number;
};

export type PlacementReplacement = {
  asOriginal: ReplacementView | null;
  asReplacement: ReplacementView | null;
};

export type CommissionStatus = "PENDING" | "PAYABLE" | "PAID" | "CANCELLED";

export type CommissionView = {
  id: string;
  placementId: string;
  candidateName: string;
  jobCode: string;
  clientName: string;
  recruiterUserId: string | null;
  recruiterName: string | null;
  amount: number | null;
  minAmount: number;
  maxAmount: number;
  status: CommissionStatus;
  payableAfter: string;
  periodDays: number;
  invoiceNumber: string | null;
  invoiceStatus: InvoiceStatus | null;
  blockers: string[];
  paidOn: string | null;
  paymentReference: string | null;
  cancelReason: string | null;
  version: number;
};

export type DashboardPipeline = {
  openJobs: number;
  candidates: number;
  availableCandidates: number;
  submissionsByStatus: Record<string, number>;
  interviewsNext7Days: number;
  offersPending: number;
  joiningsThisMonth: number;
  joiningsTrend: { month: string; count: number }[];
};

export type DashboardMoney = {
  invoicedThisMonth: number;
  collectedThisMonth: number;
  outstanding: number;
  overdueCount: number;
  overdueAmount: number;
  readyToInvoice: number;
  awaitingJoiningEmail: number;
  trend: { month: string; invoiced: number; collected: number }[];
};

export type DashboardData = {
  pipeline: DashboardPipeline | null;
  money: DashboardMoney | null;
};

export type ReportCountPoint = { month: string; total: number };
export type ReportMoneyPoint = { month: string; invoiced: number; collected: number };

export type ReportJoiningRow = {
  placementId: string;
  joiningDate: string;
  candidateName: string;
  clientName: string;
  jobCode: string;
  jobTitle: string;
  recruiter: string | null;
  offeredCtc: number;
  invoiceNumber: string | null;
  invoiceTotal: number | null;
  invoiceStatus: string | null;
};

export type ReportJoinings = {
  count: number;
  totalCtc: number;
  totalInvoiced: number | null;
  byMonth: ReportCountPoint[];
  rows: ReportJoiningRow[];
};

export type ReportAging = {
  notDue: number;
  days1to30: number;
  days31to60: number;
  days61to90: number;
  days90plus: number;
};

export type ReportRevenueRow = {
  clientId: string;
  clientName: string;
  invoiced: number;
  collected: number;
  outstanding: number;
  overdue: number;
};

export type ReportRevenue = {
  invoiced: number;
  collected: number;
  outstanding: number;
  overdue: number;
  aging: ReportAging;
  trend: ReportMoneyPoint[];
  rows: ReportRevenueRow[];
};

export type ReportRecruiterRow = {
  email: string | null;
  name: string | null;
  submissions: number;
  selected: number;
  rejected: number;
  joined: number;
  commissionTotal: number | null;
  commissionPaid: number | null;
};

export type ReportFunnel = {
  stages: { key: string; count: number }[];
  averageDaysToJoin: number | null;
};

export type ReportReplacementRow = {
  clientId: string;
  clientName: string;
  joined: number;
  exits: number;
  withinPeriod: number;
  filled: number;
  open: number;
};

export type WorkMode = "OFFICE" | "WFH";
export type AttendanceStatus = "PRESENT" | "HALF_DAY" | "ABSENT";
export type AttendanceRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

export type AttendanceToday = {
  date: string;
  dayType: "WORKING" | "WEEKLY_OFF" | "HOLIDAY";
  holidayName: string | null;
  checkedIn: boolean;
  checkedOut: boolean;
  checkIn: string | null;
  checkOut: string | null;
  workMode: WorkMode | null;
  late: boolean;
  workedMinutes: number | null;
  status: AttendanceStatus | null;
  shiftStart: string;
  optionalHolidayName: string | null;
  leaveLabel: string | null;
};

export type AttendanceDay = {
  date: string;
  state: string;
  holidayName: string | null;
  workMode: WorkMode | null;
  checkIn: string | null;
  checkOut: string | null;
  workedMinutes: number | null;
  late: boolean;
  pendingRequest: boolean;
  note: string | null;
  optionalHolidayName: string | null;
  leaveLabel: string | null;
  leaveFraction: number;
  leaveUnpaid: boolean;
};

export type AttendanceTotals = {
  present: number;
  halfDay: number;
  absent: number;
  late: number;
  wfh: number;
  holidays: number;
  weeklyOffs: number;
  leaves: number;
  lossOfPay: number;
  workedMinutes: number;
  payableDays: number;
};

export type AttendanceMonth = {
  month: string;
  userName: string;
  days: AttendanceDay[];
  totals: AttendanceTotals;
};

export type AttendanceTeamRow = {
  userId: string;
  name: string;
  email: string;
  state: string;
  workMode: WorkMode | null;
  checkIn: string | null;
  checkOut: string | null;
  workedMinutes: number | null;
  late: boolean;
  source: "SELF" | "HR" | "REQUEST" | null;
  note: string | null;
};

export type AttendanceTeamDay = {
  date: string;
  dayType: "WORKING" | "WEEKLY_OFF" | "HOLIDAY";
  holidayName: string | null;
  rows: AttendanceTeamRow[];
};

export type AttendanceSummaryRow = {
  userId: string;
  name: string;
  email: string;
  totals: AttendanceTotals;
};

export type AttendanceCorrection = {
  id: string;
  userId: string;
  userName: string;
  workDate: string;
  checkIn: string;
  checkOut: string | null;
  workMode: WorkMode;
  reason: string;
  status: AttendanceRequestStatus;
  decidedBy: string | null;
  decidedAt: string | null;
  decisionNote: string | null;
  createdAt: string;
  version: number;
};

export type AttendanceSettings = {
  shiftStart: string;
  lateGraceMinutes: number;
  halfDayMinutes: number;
  fullDayMinutes: number;
  weeklyOff: string[];
  optionalQuota: number;
  compOffValidDays: number;
  version: number;
};

export type HolidayKind = "NATIONAL" | "OPTIONAL";
export type LeaveKind = "OPTIONAL_HOLIDAY" | "COMP_OFF";

export type AttendanceHoliday = {
  id: string;
  date: string;
  name: string;
  kind: HolidayKind;
};

export type LeaveStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export type HolidayEntry = AttendanceHoliday & {
  claimed: boolean;
  leaveId: string | null;
  leaveStatus: LeaveStatus | null;
};

export type CompOffCredit = {
  id: string;
  earnedDate: string;
  expiresOn: string;
  status: "AVAILABLE" | "USED" | "EXPIRED";
  usedOn: string | null;
  source: string;
  note: string | null;
};

export type LeaveItem = {
  id: string;
  date: string;
  kind: LeaveKind;
  label: string;
  status: LeaveStatus;
};

export type LeaveOverview = {
  year: number;
  optionalQuota: number;
  optionalUsed: number;
  compOffAvailable: number;
  holidays: HolidayEntry[];
  compOffs: CompOffCredit[];
  leaves: LeaveItem[];
};

export type LeaveRow = {
  id: string;
  userId: string;
  userName: string;
  date: string;
  kind: LeaveKind;
  label: string;
  status: LeaveStatus;
  decidedBy: string | null;
  decisionNote: string | null;
  createdAt: string;
};

export type HalfDay = "FIRST" | "SECOND";

export type LeaveType = {
  id: string;
  name: string;
  yearlyDays: number;
  unpaid: boolean;
  active: boolean;
  sortOrder: number;
  version: number;
};

export type LeaveBalance = {
  leaveTypeId: string;
  name: string;
  unpaid: boolean;
  yearlyDays: number;
  used: number;
  remaining: number;
};

export type LeavePreview = {
  days: number;
  remaining: number | null;
  unpaid: boolean;
  enough: boolean;
};

export type LeaveApplication = {
  id: string;
  userId: string;
  userName: string;
  leaveTypeId: string;
  typeName: string;
  unpaid: boolean;
  fromDate: string;
  toDate: string;
  halfDay: HalfDay | null;
  days: number;
  reason: string;
  status: LeaveStatus;
  decidedBy: string | null;
  decisionNote: string | null;
  createdAt: string;
  version: number;
};

export type NotificationItem = {
  id: string;
  topic: string;
  title: string;
  body: string | null;
  link: string | null;
  read: boolean;
  createdAt: string;
};

export type NotificationFeed = {
  unread: number;
  items: NotificationItem[];
};

export type LivePayload = {
  topic: string;
  id: string | null;
  title: string | null;
  body: string | null;
  link: string | null;
  createdAt: string;
};

export type PayrollStructure = {
  id: string;
  userId: string;
  effectiveFrom: string;
  gross: number;
  basic: number | null;
  hra: number | null;
  allowance: number | null;
  pfApplicable: boolean;
  ptApplicable: boolean;
  note: string | null;
  ctcAnnual: number | null;
  variablePercent: number | null;
  variableAnnual: number | null;
  employerPf: number | null;
  gratuity: number | null;
  version: number;
};

export type PayrollBreakdown = {
  ctcAnnual: number;
  variablePercent: number;
  variableAnnual: number;
  fixedAnnual: number;
  gross: number;
  basic: number;
  hra: number;
  allowance: number;
  employerPf: number;
  gratuity: number;
};

export type PayrollEmployee = {
  userId: string;
  name: string;
  email: string;
  active: boolean;
  current: PayrollStructure | null;
};

export type PayrollSettings = {
  pfEnabled: boolean;
  pfRate: number;
  pfWageCeiling: number;
  ptAmount: number;
  ptMinGross: number;
  basicPercent: number;
  hraPercent: number;
  employerPfRate: number;
  gratuityEnabled: boolean;
  gratuityPercent: number;
  version: number;
};

export type PayrollRunStatus = "DRAFT" | "FINALIZED";

export type PayrollRun = {
  id: string;
  month: string;
  status: PayrollRunStatus;
  finalizedBy: string | null;
  finalizedAt: string | null;
  employees: number;
  totalGross: number;
  totalDeductions: number;
  totalNet: number;
  version: number;
};

export type PayslipCommission = { commissionId: string; amount: number; label: string };

export type Payslip = {
  id: string;
  runId: string;
  month: string;
  status: PayrollRunStatus;
  userId: string;
  employeeName: string;
  employeeEmail: string;
  calendarDays: number;
  payableDays: number;
  leaveDays: number;
  lossOfPayDays: number;
  gross: number;
  earnedGross: number;
  basic: number | null;
  hra: number | null;
  allowance: number | null;
  commission: number;
  otherEarnings: number;
  earningsNote: string | null;
  pf: number;
  pt: number;
  tds: number;
  otherDeductions: number;
  deductionsNote: string | null;
  totalEarnings: number;
  totalDeductions: number;
  net: number;
  commissions: PayslipCommission[];
  version: number;
};

export type PayrollDetail = { run: PayrollRun; payslips: Payslip[] };

export type SystemCheck = { key: string; ok: boolean; detail: string };

export type SystemHealth = {
  healthy: boolean;
  checkedAt: string;
  startedAt: string;
  liveConnections: number;
  checks: SystemCheck[];
};
