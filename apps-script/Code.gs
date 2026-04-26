/**
 * Attendly — Google Apps Script
 *
 * HOW TO DEPLOY:
 *  1. Open your Google Sheet.
 *  2. Click Extensions → Apps Script.
 *  3. Delete any existing code and paste this entire file.
 *  4. Click Deploy → New deployment.
 *  5. Type: Web App
 *  6. Execute as: Me
 *  7. Who has access: Anyone (even anonymous)   ← important!
 *  8. Click Deploy and copy the Web App URL.
 *  9. Paste that URL into the Android app Settings.
 *
 * Supports two modes:
 *   - Single row: { sheetName, timestamp, id }
 *   - Batch rows: { sheetName, rows: [{timestamp, id}, ...] }
 */

function doPost(e) {
  try {
    var params = JSON.parse(e.postData.contents);

    var sheetName = params.sheetName;
    if (!sheetName) {
      return respond("error", "Missing required field: sheetName");
    }

    var ss    = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName(sheetName);

    if (!sheet) {
      return respond("error", "Sheet tab not found: " + sheetName);
    }

    if (params.rows && params.rows.length > 0) {
      // ── Batch mode ──────────────────────────────────────────────────────────
      var rowsData = params.rows.map(function(r) { return [r.timestamp, r.id]; });
      var firstEmptyRow = sheet.getLastRow() + 1;
      sheet.getRange(firstEmptyRow, 1, rowsData.length, 2).setValues(rowsData);
    } else {
      // ── Single row mode ──────────────────────────────────────────────────────
      var timestamp = params.timestamp;
      var id        = params.id;
      if (!timestamp || !id) {
        return respond("error", "Missing required fields: timestamp, id");
      }
      sheet.appendRow([timestamp, id]);
    }

    return respond("success", "Row(s) appended");

  } catch (err) {
    return respond("error", err.toString());
  }
}

function respond(status, message) {
  var payload = JSON.stringify({ status: status, message: message });
  return ContentService
    .createTextOutput(payload)
    .setMimeType(ContentService.MimeType.JSON);
}

