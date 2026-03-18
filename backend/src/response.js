function sendApiSuccess(res, data, options = {}) {
  const { status = 200, fromCache, snapshotStatus, meta } = options;
  if (fromCache !== undefined) {
    res.set('X-From-Cache', String(fromCache));
  }
  if (snapshotStatus) {
    res.set('X-Snapshot-Status', snapshotStatus);
  }
  if (meta) {
    res.set('X-Backend-Meta', JSON.stringify(meta));
  }
  res.status(status).json(data);
}

function sendApiError(res, status, code, message, details = undefined) {
  res.status(status).json({
    error: {
      code,
      message,
      details,
    },
  });
}

module.exports = {
  sendApiSuccess,
  sendApiError,
};
