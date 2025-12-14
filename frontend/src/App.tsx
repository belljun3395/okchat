import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import ChatPage from './pages/ChatPage';
import AdminLayout from './layouts/AdminLayout';
import KnowledgeBaseLayout from './layouts/KnowledgeBaseLayout';
import UserManagementPage from './pages/admin/UserManagementPage';
import PathManagementPage from './pages/admin/PathManagementPage';
import UserDetailPage from './pages/admin/UserDetailPage';
import PathDetailPage from './pages/admin/PathDetailPage';
import PermissionManagementPage from './pages/admin/PermissionManagementPage';
import EmailReviewPage from './pages/admin/EmailReviewPage';
import DashboardPage from './pages/admin/DashboardPage';
import KnowledgeBaseMembersPage from './pages/admin/KnowledgeBaseMembersPage';
import PromptManagementPage from './pages/admin/PromptManagementPage';

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
        {/* Admin Routes */}
        <Route path="/admin" element={<AdminLayout />}>
          {/* Dashboard (KB List) */}
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="permissions" element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />

          {/* User Management */}
          <Route path="users" element={<UserManagementPage />} />
          <Route path="permissions/user/:email" element={<UserDetailPage />} />
          <Route path="prompts" element={<PromptManagementPage />} />
          
          {/* Legacy Redirects */}
          <Route path="knowledge-bases" element={<Navigate to="/admin/dashboard" replace />} />
        </Route>

        {/* Knowledge Base Context Routes */}
        <Route path="/admin/knowledge-bases/:id" element={<KnowledgeBaseLayout />}>
           <Route index element={<Navigate to="members" replace />} />
           <Route path="members" element={<KnowledgeBaseMembersPage />} />
           <Route path="paths" element={<PathManagementPage />} />
           <Route path="permissions" element={<PermissionManagementPage />} />
           <Route path="email" element={<EmailReviewPage />} />
           
           {/* Detailed pages might need adjustment or nested routes */}
           <Route path="path/detail" element={<PathDetailPage />} />
        </Route>
      </Routes>
    </Router>
  );
};

export default App;
