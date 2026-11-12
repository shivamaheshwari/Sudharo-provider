import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import * as jwt from 'jsonwebtoken';
import * as jwks from 'jwks-rsa';

/**
 * Validates the Supabase access token (RS256, JWKS from SUPABASE_URL/auth/v1).
 * Every protected route uses this guard + a provider-role check.
 * The Android app never decides roles or prices — the server does.
 */
@Injectable()
export class SupabaseAuthGuard implements CanActivate {
  private client = jwks({
    jwksUri: `${process.env.SUPABASE_URL}/auth/v1/.well-known/jwks.json`,
    cache: true, rateLimit: true,
  });

  async canActivate(ctx: ExecutionContext): Promise<boolean> {
    const req = ctx.switchToHttp().getRequest();
    const header = req.headers.authorization as string | undefined;
    if (!header?.startsWith('Bearer ')) throw new UnauthorizedException('Missing token');
    const token = header.slice(7);
    const decoded = jwt.decode(token, { complete: true }) as { header: { kid: string } } | null;
    if (!decoded) throw new UnauthorizedException('Bad token');
    const key = await this.client.getSigningKey(decoded.header.kid);
    const payload = jwt.verify(token, key.getPublicKey(), {
      audience: 'authenticated',
      issuer: `${process.env.SUPABASE_URL}/auth/v1`,
    }) as { sub: string; email?: string };
    req.user = { id: payload.sub, email: payload.email };
    return true;
  }
}
