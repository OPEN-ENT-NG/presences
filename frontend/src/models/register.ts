export enum RegisterState {
  TODO = 1,
  IN_PROGRESS = 2,
  DONE = 3,
}

export enum EventType {
  ABSENCE = 1,
  LATENESS = 2,
  DEPARTURE = 3,
  REMARK = 4,
}

/** Body of `POST /presences/registers` (every field is required by the back). */
export interface CreateRegisterPayload {
  course_id: string;
  structure_id: string;
  start_date: string;
  end_date: string;
  subject_id: string;
  groups: string[];
  classes: string[];
  teacherIds: string[];
  split_slot: boolean;
}

/** Response of `POST /presences/registers` (the existing register if any). */
export interface CreatedRegister {
  id: number;
  structure_id: string;
  course_id: string;
  subject_id: string;
  start_date: string;
  end_date: string;
  counsellor_input: boolean;
  state_id: RegisterState;
}

/**
 * Response of `GET /presences/registers/:id`. It has no `id`: the identifier
 * is the one of the request.
 */
export interface Register {
  /** `created by cron` when the register was created automatically */
  personnel_id: string;
  proof_id: number | null;
  course_id: string;
  /** `created by cron` when the register was created automatically */
  owner: string;
  notified: boolean;
  subject_id: string;
  /** `YYYY-MM-DDTHH:mm:ss.SSS` */
  start_date: string;
  /** `YYYY-MM-DDTHH:mm:ss.SSS` */
  end_date: string;
  structure_id: string;
  counsellor_input: boolean;
  state_id: RegisterState;
  groups: RegisterGroup[];
  teachers: RegisterTeacher[];
  students: RegisterStudent[];
}

export interface RegisterGroup {
  id: string;
  type: string;
}

export interface RegisterTeacher {
  id: string;
  displayName: string;
  functions: string[];
}

export interface RegisterStudent {
  id: string;
  /** `LASTNAME Firstname` */
  name: string;
  /** Always `null` on the recette */
  birth_day: string | null;
  group: string;
  group_name: string;
  /** Events of this register */
  events: RegisterEvent[];
  /** The day split in time slots, with the events of each one */
  day_history: RegisterHistorySlot[];
  /** Absent at the previous register of the same subject and group */
  last_course_absent: boolean;
  forgotten_notebook: boolean;
  exempted: boolean;
  exemptions: unknown[];
}

export interface RegisterHistorySlot {
  /** Slot name, e.g. `M1`; missing for an event outside the school time slots */
  name?: string;
  /** `YYYY-MM-DDTHH:mm:ss` */
  start: string;
  /** `YYYY-MM-DDTHH:mm:ss` */
  end: string;
  events: RegisterEvent[];
}

export interface RegisterEventOwner {
  id: string;
  displayName: string;
  firstName: string;
  lastName: string;
  info: string | null;
}

export interface RegisterEvent {
  id: number;
  type_id: EventType;
  /** `YYYY-MM-DDTHH:mm:ss` */
  start_date: string;
  /** `YYYY-MM-DDTHH:mm:ss` */
  end_date: string;
  /** Empty string when there is no comment */
  comment: string;
  owner: RegisterEventOwner;
  counsellor_input: boolean;
  counsellor_regularisation: boolean;
  followed: boolean;
  massmailed: boolean;
  register_id: number;
  reason_id: number | null;
}
