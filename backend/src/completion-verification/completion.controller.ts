import { Body, Controller, Param, Post, UseGuards } from '@nestjs/common';
import { SupabaseAuthGuard } from '../common/supabase-auth.guard';
import { CompletionService } from './completion.service';

@UseGuards(SupabaseAuthGuard)
@Controller('jobs/:id/completion')
export class CompletionController {
  constructor(private readonly service: CompletionService) {}

  @Post('request')
  request(@Param('id') id: string) {
    return this.service.request(id);
  }

  @Post('verify')
  verify(@Param('id') id: string, @Body() body: { otp: string }) {
    return this.service.verify(id, body.otp);
  }
}
