import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import ChatPage from './pages/ChatPage';
import AdminLayout from './layouts/AdminLayout';
import UserManagementPage from './pages/admin/UserManagementPage';
import PathManagementPage from './pages/admin/PathManagementPage';
import UserDetailPage from './pages/admin/UserDetailPage';
import PathDetailPage from './pages/admin/PathDetailPage';
import PermissionManagementPage from './pages/admin/PermissionManagementPage';
import EmailReviewPage from './pages/admin/EmailReviewPage';
import DashboardPage from './pages/admin/DashboardPage';
import KnowledgeBaseListPage from './pages/admin/KnowledgeBaseListPage';
import KnowledgeBaseMembersPage from './pages/admin/KnowledgeBaseMembersPage';

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

          {/* Knowledge Base Management */}
          <Route path="knowledge-bases" element={<KnowledgeBaseListPage />} />
          <Route path="knowledge-bases/:id/members" element={<KnowledgeBaseMembersPage />} />
          <Route path="permissions/knowledge-bases" element={<Navigate to="/admin/knowledge-bases" replace />} />

          {/* Advanced Permissions */}
          <Route path="permissions/manage" element={<PermissionManagementPage />} />

          {/* Email Review */}
          <Route path="email/review" element={<EmailReviewPage />} />
        </Route>
      </Routes>
    </Router>
  );
};

export default App;
