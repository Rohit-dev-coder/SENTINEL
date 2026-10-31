import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: 'standalone',
  // Dev only: hostnames/IPs (comma separated) allowed to load dev assets when you open the
  // dashboard by network address instead of localhost. Without this the page never hydrates.
  allowedDevOrigins: (process.env.ALLOWED_DEV_ORIGINS ?? '').split(',').map((h) => h.trim()).filter(Boolean),
};

export default nextConfig;
