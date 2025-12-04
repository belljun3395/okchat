import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import ChatPage from './pages/ChatPage';
import AdminLayout from './layouts/AdminLayout';
import UserManagementPage from './pages/admin/UserManagementPage';
import PathManagementPage from './pages/admin/PathManagementPage';
import UserDetailPage from './pages/admin/UserDetailPage';
import PathDetailPage from './pages/admin/PathDetailPage';
import PermissionManagementPage from './pages/admin/PermissionManagementPage';

import AnalyticsPage from './pages/admin/AnalyticsPage';
import EmailReviewPage from './pages/admin/EmailReviewPage';
import DashboardPage from './pages/admin/DashboardPage';

/**
 * App Component
 * 
 * The root component of the application.
 * It sets up the routing using React Router.
 */
const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<ChatPage />} />
        <Route path="/chat" element={<Navigate to="/" replace />} />

        {/* Admin Routes */}
        <Route path="/admin" element={<AdminLayout />}>
          {/* Redirect /admin to /admin/permissions (Dashboard) */}
          <Route index element={<Navigate to="/admin/permissions" replace />} />

          {/* Dashboard / Permissions Home */}
          <Route path="permissions" element={<DashboardPage />} />

          {/* User Management */}
          <Route path="users" element={<UserManagementPage />} />
          <Route path="permissions/users" element={<Navigate to="/admin/users" replace />} />
          <Route path="permissions/user/:email" element={<UserDetailPage />} />

          {/* Path Management */}
          <Route path="paths" element={<PathManagementPage />} />
          <Route path="permissions/paths" element={<Navigate to="/admin/paths" replace />} />
          <Route path="permissions/path/detail" element={<PathDetailPage />} />

          {/* Advanced Permissions */}
          <Route path="permissions/manage" element={<PermissionManagementPage />} />

          {/* Analytics */}
          <Route path="analytics" element={<AnalyticsPage />} />
          <Route path="chat/analytics" element={<Navigate to="/admin/analytics" replace />} />

          {/* Email Review */}
          <Route path="email/review" element={<EmailReviewPage />} />
        </Route>
      </Routes>
    </Router>
  );
};

export default App;
