import axios, { AxiosError } from 'axios';

/**
 * Custom API Error class
 */
export class ApiError extends Error {
    constructor(
        public status: number,
        public message: string,
        public data?: any
    ) {
        super(message);
        this.name = 'ApiError';
    }
}

/**
 * Axios instance with default configuration
 * TODO: fetch, axios 중 하나만 사용하기
 */
const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
});

/**
 * Request interceptor - Add auth headers, logging, etc.
 */
apiClient.interceptors.request.use(
    (config) => {
        // TODO: Add authentication token if needed
        // config.headers.Authorization = `Bearer ${token}`;

        console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`);
        return config;
    },
    (error) => {
        console.error('[API Request Error]', error);
        return Promise.reject(error);
    }
);

/**
 * Response interceptor - Handle errors globally
 */
apiClient.interceptors.response.use(
    (response) => {
        console.log(`[API Response] ${response.status} ${response.config.url}`);
        return response;
    },
    (error: AxiosError) => {
        const status = error.response?.status || 500;
        const message = (error.response?.data as any)?.message || error.message || 'An error occurred';
        const data = error.response?.data;

        console.error(`[API Error] ${status} ${error.config?.url}`, message);

        // Create custom error
        const apiError = new ApiError(status, message, data);

        // Handle specific error codes
        if (status === 401) {
            // Unauthorized - redirect to login or refresh token
            console.error('Unauthorized - Please login');
            // TODO: Implement auth flow
        } else if (status === 403) {
            // Forbidden
            console.error('Access denied');
        } else if (status === 404) {
            // Not found
            console.error('Resource not found');
        } else if (status >= 500) {
            // Server error
            console.error('Server error');
        }

        return Promise.reject(apiError);
    }
);

export default apiClient;
