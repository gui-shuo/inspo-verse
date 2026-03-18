export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId: string
  timestamp: number
}

export enum ApiCode {
  SUCCESS = 0,
  BAD_REQUEST = 40001,
  UNAUTHORIZED = 40100,
  FORBIDDEN = 40300,
  NOT_FOUND = 40400,
  CONFLICT = 40900,
  TOO_MANY_REQUESTS = 42900,
  INTERNAL_ERROR = 50000,
  DOWNSTREAM_UNAVAILABLE = 50300
}
