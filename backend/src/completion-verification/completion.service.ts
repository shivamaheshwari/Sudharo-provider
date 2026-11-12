import { BadRequestException, Injectable } from '@nestjs/common';
import { randomInt, createHash } from 'crypto';

/**
 * Job-completion OTP — SEPARATE from auth OTP (Supabase).
 * Flow: provider taps Request Completion -> server creates a 6-digit OTP
 * (5 min expiry, max 5 attempts), sends it to the CUSTOMER (not the
 * provider), customer shares it after checking the work, provider enters
 * it, server verifies + marks job COMPLETED in a transaction + audit log.
 * Plain OTP is never stored — only its SHA-256 hash.
 */
@Injectable()
export class CompletionService {
  // Prisma client is injected in the real app; kept in-memory here so the
  // module compiles standalone — wire PrismaService before deploying.
  private store = new Map<string, { hash: string; expiresAt: number; attempts: number }>();

  request(jobId: string) {
    const otp = String(randomInt(100000, 999999));
    const hash = createHash('sha256').update(otp).digest('hex');
    this.store.set(jobId, { hash, expiresAt: Date.now() + 5 * 60 * 1000, attempts: 0 });
    // TODO: send `otp` to the customer via FCM/SMS (never to the provider),
    // and persist CompletionVerification + AuditLog via Prisma.
    // In dev, log only that an OTP was issued — never log the OTP itself in prod.
    return { ok: true, message: 'Completion OTP sent to customer' };
  }

  verify(jobId: string, otp: string) {
    const rec = this.store.get(jobId);
    if (!rec) throw new BadRequestException('No completion request for this job');
    if (Date.now() > rec.expiresAt) throw new BadRequestException('OTP expired');
    if (++rec.attempts > 5) throw new BadRequestException('Too many attempts');
    if (createHash('sha256').update(otp).digest('hex') !== rec.hash) {
      throw new BadRequestException('Wrong OTP');
    }
    this.store.delete(jobId);
    // TODO: in a Prisma transaction: mark CompletionVerification.verified,
    // Job.status=COMPLETED, write AuditLog(OTP_VERIFIED / JOB_COMPLETED).
    return { ok: true, message: 'Job completed' };
  }
}
