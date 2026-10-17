export type ExportCell = string | number | null | undefined;

export async function downloadXlsx(fileName: string, sheetName: string, headers: string[], rows: ExportCell[][]) {
  const { Workbook } = await import("exceljs");
  const workbook = new Workbook();
  const sheet = workbook.addWorksheet(sheetName.slice(0, 31) || "Report");
  sheet.addRow(headers);
  rows.forEach((row) => sheet.addRow(row.map((value) => (value === undefined ? null : value))));
  const head = sheet.getRow(1);
  head.font = { bold: true };
  head.alignment = { vertical: "middle" };
  sheet.views = [{ state: "frozen", ySplit: 1 }];
  sheet.columns.forEach((column, index) => {
    let width = String(headers[index] ?? "").length;
    rows.forEach((row) => {
      width = Math.max(width, String(row[index] ?? "").length);
    });
    column.width = Math.min(Math.max(width + 2, 10), 48);
  });
  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
