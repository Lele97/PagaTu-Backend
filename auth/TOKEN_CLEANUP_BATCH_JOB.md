# Token Cleanup Batch Job Documentation

## Overview

The Token Cleanup Batch Job is a Spring Boot scheduled task that automatically cleans up expired password reset tokens every 2 hours. It ensures that tokens with `TokenStatus.ACTIVE` that have passed their `expiredDate` are updated to `TokenStatus.EXPIRED`.

## Components

### 1. TokenCleanupBatchJob
- **Location**: `com.pagatu.auth.batch.TokenCleanupBatchJob`
- **Frequency**: Runs every 2 hours (7,200,000 milliseconds)
- **Purpose**: Main batch job component that handles token cleanup

### 2. TokenCleanupMonitoringService
- **Location**: `com.pagatu.auth.service.TokenCleanupMonitoringService`
- **Purpose**: Provides monitoring and statistics for token cleanup operations

### 3. BatchJobConfig
- **Location**: `com.pagatu.auth.config.BatchJobConfig`
- **Purpose**: Configuration for batch job task scheduler and retry mechanisms

### 4. Enhanced Repository Methods
- **Location**: `com.pagatu.auth.repository.TokenForUserPasswordResetRepository`
- **New Methods**:
  - `findExpiredActiveTokens()`: Find tokens that are active but expired
  - `findAllByTokenStatus()`: Find all tokens by status
  - `updateExpiredTokensStatus()`: Batch update expired tokens

## Features

### 1. Dual Processing Strategy
- **Batch Update**: Primary method using a single SQL query for efficiency
- **Individual Processing**: Fallback method when batch update fails or returns 0 results

### 2. Robust Error Handling
- Retry mechanism with exponential backoff (max 3 attempts)
- Individual token processing continues even if single tokens fail
- Comprehensive error logging

### 3. Comprehensive Logging
- Job start/completion with execution time
- Initial and final token statistics
- Processing counts (processed vs updated)
- Error details with context

### 4. Monitoring & Statistics
- Token counts by status (ACTIVE, EXPIRED)
- Expired active token detection
- Pre-processing validation

## Configuration

### Application Properties
The batch job is enabled by default. To disable it, add:
```properties
spring.scheduling.enabled=false
```

### Annotations Required
The following annotations must be present in the main application class:
- `@EnableScheduling`: Enables scheduled tasks
- `@EnableRetry`: Enables retry functionality

## Usage

### Automatic Execution
The batch job runs automatically every 2 hours once the application starts.

### Manual Execution
For testing or immediate cleanup:
```java
@Autowired
private TokenCleanupBatchJob batchJob;

// Trigger manual cleanup
batchJob.manualTrigger();
```

### Monitoring
```java
@Autowired
private TokenCleanupMonitoringService monitoringService;

// Get current statistics
TokenStatistics stats = monitoringService.getTokenStatistics();
System.out.println(stats);

// Check if cleanup is needed
boolean needsCleanup = monitoringService.hasTokensToCleanup();
```

## Logging

### Log Levels
- **INFO**: Job execution status, statistics, successful operations
- **DEBUG**: Detailed processing information, individual token updates
- **ERROR**: Failures, retry attempts, exceptions

### Key Log Messages
- `"Starting token cleanup batch job at {timestamp}"`
- `"Token cleanup batch job completed successfully. Processed: {count}, Updated: {count}"`
- `"No expired active tokens found, skipping cleanup"`
- `"Token cleanup batch job failed after {time} ms"`

## Performance Considerations

### Batch Update Optimization
The job prioritizes batch updates for better performance:
1. Single SQL query updates all expired tokens
2. Reduces database round trips
3. Faster execution for large datasets

### Fallback Strategy
If batch update fails or returns unexpected results:
1. Falls back to individual token processing
2. Provides more detailed logging per token
3. Ensures robustness over pure performance

## Error Recovery

### Retry Mechanism
- **Database Operations**: 3 retry attempts with exponential backoff
- **Individual Tokens**: Processing continues even if individual tokens fail
- **Batch Operations**: Falls back to individual processing on failure

### Transaction Management
- Uses `secondTransactionManager` for all database operations
- Ensures consistency with the existing transaction configuration
- Rollback on failure prevents partial updates

## Testing

### Unit Tests
Location: `com.pagatu.auth.batch.TokenCleanupBatchJobTest`

Test scenarios:
- Successful batch update
- No tokens to cleanup
- Fallback to individual processing
- Manual trigger functionality

### Integration Testing
1. Create test tokens with past expiration dates
2. Run manual trigger
3. Verify tokens are updated to EXPIRED status
4. Check logs for proper execution

## Monitoring Dashboard (Future Enhancement)

Potential metrics to expose:
- Total tokens processed per execution
- Average execution time
- Success/failure rates
- Token cleanup trends over time

## Troubleshooting

### Common Issues

1. **Job Not Running**
   - Check `@EnableScheduling` annotation
   - Verify application context startup
   - Check for scheduling conflicts

2. **Database Connection Issues**
   - Verify `secondTransactionManager` configuration
   - Check database connectivity
   - Review connection pool settings

3. **Performance Issues**
   - Monitor batch update efficiency
   - Consider database indexing on `expiredDate` and `tokenStatus`
   - Adjust retry backoff settings if needed

### Health Checks
Monitor the following:
- Execution frequency (every 2 hours)
- Processing times (should be < 1 minute for normal loads)
- Error rates (should be < 1%)
- Token accumulation (expired active tokens should not accumulate)
