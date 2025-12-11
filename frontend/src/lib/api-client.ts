import axios, {AxiosError} from 'axios';

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

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Axios instance with default configuration
 */
const apiClient = axios.create({
    baseURL: BASE_URL,
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
});

/**
 * Stream request wrapper using fetch for SSE
 * Uses the same base URL and configuration as axios instance
 */
/**
 * Stream request wrapper using Axios for SSE
 * Ensures we use the same interceptors and config as the main client
 */
export const streamRequest = async (url: string, data: any, customHeaders: any = {}) => {
    return await apiClient.post(url, data, {
        headers: {
            ...customHeaders,
            'Accept': 'text/event-stream'
        },
        responseType: 'stream',
        adapter: 'fetch' // Use fetch adapter to get ReadableStream
    });
};

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
