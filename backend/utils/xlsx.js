// utils/xlsx.js — build a real .xlsx workbook of group members (exceljs).

const ExcelJS = require('exceljs');

/**
 * @param {string} groupName  e.g. "G01"
 * @param {Array<{name,student_number,programme_code,status}>} members
 * @returns {Promise<Buffer>} xlsx file bytes
 */
async function buildMembersWorkbook(groupName, members) {
  const wb = new ExcelJS.Workbook();
  wb.creator = 'Campus Companion';
  wb.created = new Date();

  const ws = wb.addWorksheet((groupName || 'Group').substring(0, 28));
  ws.columns = [
    { header: 'Name', key: 'name', width: 28 },
    { header: 'Student Number', key: 'student_number', width: 16 },
    { header: 'Programme', key: 'programme_code', width: 12 },
    { header: 'Group', key: 'group', width: 10 },
    { header: 'Status', key: 'status', width: 12 },
  ];
  ws.getRow(1).font = { bold: true };

  for (const m of members || []) {
    ws.addRow({
      name: m.name,
      student_number: m.student_number,
      programme_code: m.programme_code,
      group: groupName || '',
      status: m.status || '',
    });
  }

  const buf = await wb.xlsx.writeBuffer();
  return Buffer.from(buf);
}

module.exports = { buildMembersWorkbook };
