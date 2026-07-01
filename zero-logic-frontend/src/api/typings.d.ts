declare namespace API {
  type AppAddRequest = {
    initPrompt?: string
    attachmentId?: number
  }

  type AppAdminUpdateRequest = {
    id?: number
    appName?: string
    cover?: string
    priority?: number
  }

  type AppDeployRequest = {
    appId?: number
  }

  type AppVersionDeployRequest = {
    appId?: number | string
    versionId?: number | string
  }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    priority?: number
    userId?: number
  }

  type AppUpdateRequest = {
    id?: number
    appName?: string
  }

  type AppVO = {
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: number
    createTime?: string
    updateTime?: string
    promptAttachmentVO?: promptAttachmentVO
    user?: UserVO
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponseListProjectVersionVO = {
    code?: number
    data?: ProjectVersionVO[]
    message?: string
  }

  type BaseResponseListDeployRecordVO = {
    code?: number
    data?: DeployRecordVO[]
    message?: string
  }

  type BaseResponseGenerationDashboardVO = {
    code?: number
    data?: GenerationDashboardVO
    message?: string
  }

  type BaseResponsePageAppVO = {
    code?: number
    data?: PageAppVO
    message?: string
  }

  type BaseResponsePageChatHistory = {
    code?: number
    data?: PageChatHistory
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type ChatHistory = {
    id?: number
    taskId?: number | string
    message?: string
    messageType?: string
    appId?: number
    userId?: number
    promptAttachmentVO?: promptAttachmentVO
    ragRetrieval?: RagRetrievalVO
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type ChatHistoryQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    message?: string
    messageType?: string
    appId?: number
    userId?: number
    lastCreateTime?: string
  }

  type chatToGenCodeParams = {
    appId: number
    message: string
  }

  type DeleteRequest = {
    id?: number
  }

  type downloadAppCodeParams = {
    appId: number
  }

  type getAppVOByIdByAdminParams = {
    id: number
  }

  type getAppVOByIdParams = {
    id: number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type getGenerationDashboardParams = {
    appId?: number | string
  }

  type DailyGenerationStatVO = {
    date?: string
    taskCount?: number
    successCount?: number
    failedCount?: number
  }

  type GenerationDashboardVO = {
    totalTaskCount?: number
    successTaskCount?: number
    failedTaskCount?: number
    runningTaskCount?: number
    successRate?: number
    totalTokenUsage?: number
    totalToolCallCount?: number
    avgDurationSeconds?: number
    buildSuccessCount?: number
    buildFailedCount?: number
    repairTotalCount?: number
    repairSuccessCount?: number
    highRiskToolCallCount?: number
    dailyStats?: DailyGenerationStatVO[]
  }

  type listAppChatHistoryParams = {
    appId: number
    pageSize?: number
    lastCreateTime?: string
  }

  type listAppVersionsParams = {
    appId: number | string
  }

  type listDeployRecordsParams = {
    appId: number | string
  }

  type LoginUserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
    updateTime?: string
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageChatHistory = {
    records?: ChatHistory[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type ServerSentEventString = true

  type serveStaticResourceParams = {
    deployKey: string
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserProfileUpdateRequest = {
    userName?: string
    userAvatar?: string
    userProfile?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }

  type promptAttachmentVO = {
    id?: number
    fileName?: string
    fileExtension?: string
    contentType?: string
    fileSize?: number
  }

  type GenerationTaskVO = {
    id?: number | string
    appId?: number
    userId?: number
    attachmentId?: number
    taskType?: string
    status?: string
    currentStep?: string
    inputPrompt?: string
    codeGenType?: string
    errorMessage?: string
    tokenUsage?: number
    toolCallCount?: number
    ragRetrieval?: RagRetrievalVO
    latestBuild?: GenerationBuildRecordVO
    repairs?: GenerationRepairRecordVO[]
    toolCalls?: ToolCallRecordVO[]
    startTime?: string
    endTime?: string
    createTime?: string
    updateTime?: string
  }

  type RagReferenceVO = {
    documentId?: number | string
    chunkId?: number | string
    documentName?: string
    chunkIndex?: number
    contentSnippet?: string
    score?: number
  }

  type GenerationBuildRecordVO = {
    id?: number | string
    attemptNo?: number
    status?: string
    command?: string
    exitCode?: number
    logText?: string
    durationMs?: number
    timedOut?: boolean
    artifactPath?: string
    createTime?: string
  }

  type GenerationRepairRecordVO = {
    id?: number | string
    repairAttempt?: number
    sourceBuildRecordId?: number | string
    status?: string
    errorSummary?: string
    suspectedFiles?: string[]
    changedFiles?: string[]
    aiResponse?: string
    errorMessage?: string
    durationMs?: number
    createTime?: string
  }

  type ToolCallRecordVO = {
    id?: number | string
    toolName?: string
    displayName?: string
    toolCategory?: string
    riskLevel?: string
    callSource?: string
    status?: string
    argumentsJson?: string
    resultSummary?: string
    errorMessage?: string
    durationMs?: number
    createTime?: string
  }

  type ProjectVersionVO = {
    id?: number | string
    appId?: number | string
    taskId?: number | string
    versionNo?: number
    versionName?: string
    codeGenType?: string
    status?: string
    buildRecordId?: number | string
    createTime?: string
  }

  type DeployRecordVO = {
    id?: number | string
    appId?: number | string
    versionId?: number | string
    deployKey?: string
    deployUrl?: string
    deployType?: string
    status?: string
    errorMessage?: string
    createTime?: string
  }

  type RagRetrievalVO = {
    taskId?: number | string
    queryText?: string
    topK?: number
    hitCount?: number
    injectedCharLength?: number
    references?: RagReferenceVO[]
    createTime?: string
  }




}
