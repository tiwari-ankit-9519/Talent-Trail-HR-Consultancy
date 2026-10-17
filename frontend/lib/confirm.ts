type Handler = (message: string) => Promise<boolean>;

let handler: Handler | null = null;

export function registerConfirm(next: Handler | null) {
  handler = next;
}

export function confirmDialog(message: string): Promise<boolean> {
  if (handler) return handler(message);
  return Promise.resolve(typeof window !== "undefined" ? window.confirm(message) : false);
}
