"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ApiError, deleteUploadedAsset, uploadAsset } from "@/lib/api";
import type { Template, UploadedAsset } from "@/lib/types";

export type MaterialSlot = {
  asset: UploadedAsset | null;
  uploading: boolean;
  error: string | null;
  localPreviewUrl: string | null;
};

function emptySlots(count: number): MaterialSlot[] {
  return Array.from({ length: count }, () => ({
    asset: null,
    uploading: false,
    error: null,
    localPreviewUrl: null,
  }));
}

function normalizeMimeType(mimeType: string): string {
  const normalized = mimeType.toLowerCase();
  return normalized === "image/jpg" ? "image/jpeg" : normalized;
}

export function formatFileSize(bytes: number): string {
  if (bytes >= 1024 * 1024) {
    return `${Math.round(bytes / (1024 * 1024))} MB`;
  }
  return `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

export function formatAllowedTypes(template: Template): string {
  return template.allowedMimeTypes
    .map((mime) => {
      if (mime === "image/jpeg" || mime === "image/jpg") return "JPG";
      if (mime === "image/png") return "PNG";
      if (mime === "image/webp") return "WebP";
      return mime;
    })
    .filter((mime, index, list) => list.indexOf(mime) === index)
    .join("、");
}

function validateFile(file: File, template: Template): string | null {
  if (file.size <= 0) return "图片文件为空，请重新选择";
  const mimeType = normalizeMimeType(file.type);
  const allowed = template.allowedMimeTypes.map(normalizeMimeType);
  if (!allowed.includes(mimeType)) {
    return `格式不支持，请选择 ${formatAllowedTypes(template)} 图片`;
  }
  if (file.size > template.maxImageSizeBytes) {
    return `图片不能超过 ${formatFileSize(template.maxImageSizeBytes)}`;
  }
  return null;
}

export function useMaterialUploads(template: Template) {
  const [slots, setSlots] = useState<MaterialSlot[]>(() => emptySlots(template.requiredImageCount));
  const slotsRef = useRef(slots);
  const controllers = useRef<Array<AbortController | null>>([]);
  const requestTokens = useRef<number[]>(Array.from({ length: template.requiredImageCount }, () => 0));
  const objectUrls = useRef(new Set<string>());
  const mounted = useRef(true);

  useEffect(() => {
    slotsRef.current = slots;
  }, [slots]);

  const revokeObjectUrl = useCallback((url: string | null) => {
    if (!url) return;
    URL.revokeObjectURL(url);
    objectUrls.current.delete(url);
  }, []);

  useEffect(() => {
    mounted.current = true;
    const activeControllers = controllers.current;
    const activeUrls = objectUrls.current;
    return () => {
      mounted.current = false;
      activeControllers.forEach((controller) => controller?.abort());
      activeUrls.forEach((url) => URL.revokeObjectURL(url));
      activeUrls.clear();
    };
  }, []);

  const updateSlot = useCallback((index: number, update: (slot: MaterialSlot) => MaterialSlot) => {
    setSlots((current) => {
      const next = current.map((slot, slotIndex) => slotIndex === index ? update(slot) : slot);
      slotsRef.current = next;
      return next;
    });
  }, []);

  const selectFile = useCallback(async (index: number, file: File) => {
    const validationError = validateFile(file, template);
    if (validationError) {
      updateSlot(index, (slot) => ({ ...slot, error: validationError }));
      return;
    }

    const previousSlot = slotsRef.current[index];
    const previousAsset = previousSlot?.asset ?? null;
    const token = (requestTokens.current[index] ?? 0) + 1;
    requestTokens.current[index] = token;
    controllers.current[index]?.abort();
    const controller = new AbortController();
    controllers.current[index] = controller;

    revokeObjectUrl(previousSlot?.localPreviewUrl ?? null);
    const localPreviewUrl = URL.createObjectURL(file);
    objectUrls.current.add(localPreviewUrl);
    updateSlot(index, (slot) => ({
      ...slot,
      uploading: true,
      error: null,
      localPreviewUrl,
    }));

    try {
      const asset = await uploadAsset(template.id, file, controller.signal);
      if (!mounted.current || requestTokens.current[index] !== token) return;
      revokeObjectUrl(localPreviewUrl);
      updateSlot(index, () => ({
        asset,
        uploading: false,
        error: null,
        localPreviewUrl: null,
      }));
      if (previousAsset && previousAsset.id !== asset.id) {
        void deleteUploadedAsset(previousAsset.id).catch(() => undefined);
      }
    } catch (cause) {
      if (!mounted.current || requestTokens.current[index] !== token) return;
      revokeObjectUrl(localPreviewUrl);
      const message = cause instanceof ApiError
        ? cause.message
        : cause instanceof Error
          ? cause.message
          : "图片上传失败，请重试";
      updateSlot(index, (slot) => ({
        ...slot,
        uploading: false,
        error: message,
        localPreviewUrl: null,
      }));
    }
  }, [revokeObjectUrl, template, updateSlot]);

  const removeFile = useCallback((index: number) => {
    requestTokens.current[index] = (requestTokens.current[index] ?? 0) + 1;
    controllers.current[index]?.abort();
    controllers.current[index] = null;

    const slot = slotsRef.current[index];
    revokeObjectUrl(slot?.localPreviewUrl ?? null);
    updateSlot(index, () => ({
      asset: null,
      uploading: false,
      error: null,
      localPreviewUrl: null,
    }));
    if (slot?.asset) {
      void deleteUploadedAsset(slot.asset.id).catch(() => undefined);
    }
  }, [revokeObjectUrl, updateSlot]);

  const readyCount = useMemo(
    () => slots.filter((slot) => slot.asset !== null && !slot.uploading).length,
    [slots],
  );
  const uploadingCount = useMemo(() => slots.filter((slot) => slot.uploading).length, [slots]);
  const allReady = readyCount === template.requiredImageCount && uploadingCount === 0;

  const validateSelection = useCallback((): string | null => {
    const currentSlots = slotsRef.current;
    if (currentSlots.length !== template.requiredImageCount) {
      return `需要准备 ${template.requiredImageCount} 张图片`;
    }
    if (currentSlots.some((slot) => slot.uploading)) {
      return "还有图片正在上传，请等待完成后再提交";
    }
    const missingCount = currentSlots.filter((slot) => slot.asset === null).length;
    if (missingCount > 0) {
      return `还需要上传 ${missingCount} 张图片`;
    }
    const allowed = template.allowedMimeTypes.map(normalizeMimeType);
    for (const slot of currentSlots) {
      if (!slot.asset) continue;
      if (!allowed.includes(normalizeMimeType(slot.asset.mimeType))) {
        return "有图片格式不符合模板要求，请替换后重试";
      }
      if (slot.asset.sizeBytes > template.maxImageSizeBytes) {
        return `有图片超过 ${formatFileSize(template.maxImageSizeBytes)}，请替换后重试`;
      }
    }
    return null;
  }, [template]);

  return {
    slots,
    readyCount,
    uploadingCount,
    allReady,
    selectFile,
    removeFile,
    validateSelection,
  };
}
