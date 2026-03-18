const { sendApiError } = require('./response');

const adminEnabled = () =>
  process.env.ENABLE_ADMIN === 'true' || process.env.NODE_ENV !== 'production';

function adminGuard(req, res, next) {
  if (!adminEnabled()) {
    return sendApiError(res, 404, 'admin_disabled', 'Admin endpoints are disabled.');
  }

  const secret = process.env.ADMIN_SECRET;
  if (secret) {
    const provided = req.get('x-admin-secret') || req.query.secret;
    if (provided !== secret) {
      return sendApiError(res, 401, 'admin_unauthorized', 'Missing or invalid admin secret.');
    }
  }

  next();
}

module.exports = { adminGuard, adminEnabled };
