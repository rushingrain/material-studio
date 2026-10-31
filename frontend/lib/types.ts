export type TaskStatus = "PENDING" | "PROCESSING" | "SUCCEEDED" | "FAILED";

export type TaskStage =
  | "QUEUED"
  | "ASSET_CHECK"
  | "SIMULATING"
  | "RESULT_PACKAGING"
  | "COMPLETED"
  | "FAILED";

export type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

export type Template = {
  id: number;
  code: string;
  name: string;
  category: string;
  description: string;
  coverUrl: string;
  exampleImageUrl: string;
  requiredImageCount: number;
  allowedMimeTypes: string[];
  maxImageSizeBytes: number;
  sortOrder: number;
  active: boolean;
};

export type AppUser = {
  id: number;
  nickname: string;
  createdAt: string;
};

export type TaskAsset = {
  id: number;
  taskId: string;
  originalFilename: string;
  mimeType: string;
  sizeBytes: number;
  sortOrder: number;
};

export type UploadedAsset = {
  id: string;
  url: string;
  originalFilename: string;
  mimeType: string;
  sizeBytes: number;
};

export type Task = {
  id: string;
  userId: number;
  templateId: number;
  status: TaskStatus;
  stage: TaskStage;
  failureStage: TaskStage | null;
  failureReason: string | null;
  resultUrl: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
};

export type ApiState<T> =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; data: T }
  | { status: "error"; error: Error };
