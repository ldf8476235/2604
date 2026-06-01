const SUPPORTED_COMPRESS_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);
const TARGET_BYTES = 500 * 1024;
const MAX_EDGE = 1920;
const INITIAL_QUALITY = 0.78;
const MIN_QUALITY = 0.58;
const QUALITY_STEP = 0.08;

export type ImageCompressionResult = {
  file: File;
  compressed: boolean;
  originalSize: number;
  outputSize: number;
};

export async function prepareImageForUpload(file: File): Promise<ImageCompressionResult> {
  if (!canCompressImage(file)) {
    return buildPassthroughResult(file);
  }
  if (typeof document === "undefined" || typeof createImageBitmap !== "function") {
    return buildPassthroughResult(file);
  }
  try {
    const outputType = await resolveOutputType();
    const bitmap = await createImageBitmap(file);
    try {
      const { width, height } = calculateTargetSize(bitmap.width, bitmap.height);
      const canvas = document.createElement("canvas");
      canvas.width = width;
      canvas.height = height;
      const context = canvas.getContext("2d", { alpha: outputType === "image/webp" });
      if (!context) {
        return buildPassthroughResult(file);
      }
      if (outputType === "image/jpeg") {
        context.fillStyle = "#ffffff";
        context.fillRect(0, 0, width, height);
      }
      context.drawImage(bitmap, 0, 0, width, height);
      const blob = await renderBestBlob(canvas, outputType);
      if (!blob) {
        return buildPassthroughResult(file);
      }
      const nextFile = new File([blob], replaceImageExtension(file.name, outputType), {
        type: outputType,
        lastModified: Date.now(),
      });
      if (nextFile.size >= file.size && width === bitmap.width && height === bitmap.height) {
        return buildPassthroughResult(file);
      }
      return {
        file: nextFile,
        compressed: true,
        originalSize: file.size,
        outputSize: nextFile.size,
      };
    } finally {
      bitmap.close();
    }
  } catch {
    return buildPassthroughResult(file);
  }
}

function canCompressImage(file: File) {
  return SUPPORTED_COMPRESS_TYPES.has(file.type);
}

function buildPassthroughResult(file: File): ImageCompressionResult {
  return {
    file,
    compressed: false,
    originalSize: file.size,
    outputSize: file.size,
  };
}

function calculateTargetSize(width: number, height: number) {
  const longest = Math.max(width, height);
  if (longest <= MAX_EDGE) {
    return { width, height };
  }
  const scale = MAX_EDGE / longest;
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  };
}

async function resolveOutputType() {
  if (await isWebpSupported()) {
    return "image/webp";
  }
  return "image/jpeg";
}

function isWebpSupported() {
  const canvas = document.createElement("canvas");
  canvas.width = 1;
  canvas.height = 1;
  return canvasToBlob(canvas, "image/webp", 0.8).then((blob) => blob?.type === "image/webp");
}

async function renderBestBlob(canvas: HTMLCanvasElement, type: string) {
  let bestBlob: Blob | null = null;
  const qualityLevels = buildQualityLevels();
  for (const quality of qualityLevels) {
    const blob = await canvasToBlob(canvas, type, quality);
    if (!blob) {
      continue;
    }
    bestBlob = blob;
    if (blob.size <= TARGET_BYTES) {
      return blob;
    }
  }
  return bestBlob;
}

function buildQualityLevels() {
  const result: number[] = [];
  for (let quality = INITIAL_QUALITY; quality > MIN_QUALITY; quality -= QUALITY_STEP) {
    result.push(Number(quality.toFixed(2)));
  }
  result.push(MIN_QUALITY);
  return Array.from(new Set(result));
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality: number) {
  return new Promise<Blob | null>((resolve) => {
    canvas.toBlob((blob) => resolve(blob), type, quality);
  });
}

function replaceImageExtension(filename: string, type: string) {
  const extension = type === "image/webp" ? ".webp" : ".jpg";
  const basename = filename.replace(/\.[^.]+$/, "");
  return `${basename || "image"}${extension}`;
}
