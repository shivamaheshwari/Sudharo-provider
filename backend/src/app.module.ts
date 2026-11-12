import { Module } from '@nestjs/common';
import { AuthModule } from './auth/auth.module';
import { RequirementsModule } from './requirements/requirements.module';
import { BidsModule } from './bids/bids.module';
import { JobsModule } from './jobs/jobs.module';
import { QuotationsModule } from './quotations/quotations.module';
import { CompletionModule } from './completion-verification/completion.module';
import { ProvidersModule } from './providers/providers.module';

@Module({
  imports: [AuthModule, ProvidersModule, RequirementsModule, BidsModule, JobsModule, QuotationsModule, CompletionModule],
})
export class AppModule {}
