import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { permissionService, userService } from '../../services';
import type { PathDetailResponse, User } from '../../types';

interface TreeNode {
    name: string;
    fullPath: string;
    children: Map<string, TreeNode>;
    isLeaf: boolean;
}

interface Override {
    id: string;
    userEmail: string;
    userName: string;
    permission: 'READ' | 'DENY';
}

const PermissionManagementPage: React.FC = () => {
    const [paths, setPaths] = useState<string[]>([]);
    const [selectedPath, setSelectedPath] = useState<string>('');
    const [pathDetail, setPathDetail] = useState<PathDetailResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [expandedPaths, setExpandedPaths] = useState<Set<string>>(new Set());

    const [defaultAccess, setDefaultAccess] = useState<'READ' | 'DENY'>('READ');
    const [inherit, setInherit] = useState(true);
    const [overrides, setOverrides] = useState<Override[]>([]);
    const [showAddOverride, setShowAddOverride] = useState(false);
    const [newOverride, setNewOverride] = useState({ userEmail: '', permission: 'READ' as const });

    // Users list for dropdown
    const [users, setUsers] = useState<User[]>([]);
    const [userSearchTerm, setUserSearchTerm] = useState('');

    const fetchPaths = useCallback(async () => {
        try {
            setLoading(true);
            const [pathsResponse, usersResponse] = await Promise.all([
                permissionService.getAllPaths(),
                userService.getAll()
            ]);
            setPaths(pathsResponse.data);
            setUsers(usersResponse.data);
            if (pathsResponse.data.length > 0 && !selectedPath) {
                setSelectedPath(pathsResponse.data[0]);
            }
        } catch (err) {
            console.error('Failed to fetch data:', err);
        } finally {
            setLoading(false);
        }
    }, [selectedPath]);

    useEffect(() => {
        fetchPaths();
    }, [fetchPaths]);

    useEffect(() => {
        if (selectedPath) {
            fetchPathDetail(selectedPath);
        }
    }, [selectedPath]);

    const fetchPathDetail = async (path: string) => {
        try {
            const response = await permissionService.getPathDetail(path);
            setPathDetail(response.data);
            // Initialize overrides from users with access
            const initialOverrides: Override[] = response.data.usersWithAccess.map((user, idx) => ({
                id: `override-${idx}`,
                userEmail: user.email,
                userName: user.name,
                permission: 'READ' // Default permission type
            }));
            setOverrides(initialOverrides);
        } catch (err) {
            console.error('Failed to fetch path detail:', err);
        }
    };

    // Build tree structure from paths
    const treeRoot = useMemo(() => {
        const root: TreeNode = { name: 'Root', fullPath: '', children: new Map(), isLeaf: false };

        paths.forEach(path => {
            const parts = path.split('>').map(p => p.trim()).filter(p => p);
            let current = root;

            parts.forEach((part, index) => {
                const fullPath = parts.slice(0, index + 1).join(' > ');

                if (!current.children.has(part)) {
                    current.children.set(part, {
                        name: part,
                        fullPath,
                        children: new Map(),
                        isLeaf: index === parts.length - 1
                    });
                }
                current = current.children.get(part)!;
            });
        });

        return root;
    }, [paths]);

    // Filter paths based on search term
    const filteredPaths = useMemo(() => {
        if (!searchTerm) return paths;
        const term = searchTerm.toLowerCase();
        return paths.filter(path => path.toLowerCase().includes(term));
    }, [paths, searchTerm]);

    // Filter users based on search term
    const filteredUsers = useMemo(() => {
        if (!userSearchTerm) return users;
        const term = userSearchTerm.toLowerCase();
        return users.filter(user =>
            user.name.toLowerCase().includes(term) ||
            user.email.toLowerCase().includes(term)
        );
    }, [users, userSearchTerm]);

    const toggleExpand = (path: string) => {
        setExpandedPaths(prev => {
            const next = new Set(prev);
            if (next.has(path)) {
                next.delete(path);
            } else {
                next.add(path);
            }
            return next;
        });
    };

    // Filter tree based on search term
    const shouldShowNode = (node: TreeNode): boolean => {
        if (!searchTerm) return true;
        const term = searchTerm.toLowerCase();
        return node.fullPath.toLowerCase().includes(term) ||
               node.name.toLowerCase().includes(term) ||
               Array.from(node.children.values()).some(child => shouldShowNode(child));
    };

    // Render tree node as table rows
    const renderTreeNode = (node: TreeNode, level: number = 0): React.ReactNode[] => {
        if (!shouldShowNode(node)) return [];

        const hasChildren = node.children.size > 0;
        const isExpanded = expandedPaths.has(node.fullPath);
        const isSelected = selectedPath === node.fullPath;
        const indentSize = level > 1 ? (level - 1) * 24 : 0;
        const isRoot = level === 0;
        const isFirstLevel = level === 1;

        const nodes: React.ReactNode[] = [];

        // Render current node (skip root)
        if (!isRoot) {
            nodes.push(
                <tr
                    key={node.fullPath}
                    className={`path-row path-row-level-${level} ${isFirstLevel ? 'path-row-first-level' : ''} ${isSelected ? 'path-row-selected' : ''}`}
                    onClick={() => {
                        if (node.fullPath) {
                            setSelectedPath(node.fullPath);
                        }
                    }}
                    style={{
                        cursor: 'pointer',
                        backgroundColor: isSelected ? '#EFF6FF' : undefined,
                        borderLeft: isSelected ? '4px solid #3B82F6' : '4px solid transparent'
                    }}
                >
                    <td className="path-cell" style={{ paddingLeft: `${indentSize + 16}px` }}>
                        <div className="flex items-center gap-3">
                            {hasChildren ? (
                                <button
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        toggleExpand(node.fullPath);
                                    }}
                                    className="tree-toggle-btn"
                                    title={isExpanded ? 'Collapse' : 'Expand'}
                                >
                                    <span
                                        className="tree-toggle-icon"
                                        style={{
                                            transform: isExpanded ? 'rotate(0deg)' : 'rotate(-90deg)',
                                        }}
                                    >
                                        ▼
                                    </span>
                                </button>
                            ) : (
                                <span className="tree-icon-spacer"></span>
                            )}

                            <div className="path-node-content flex items-center gap-2 flex-1">
                                {hasChildren ? (
                                    <span className="path-icon path-icon-folder">
                                        {isExpanded ? '📂' : '📁'}
                                    </span>
                                ) : (
                                    <span className="path-icon path-icon-file">📄</span>
                                )}

                                <span className="path-segment-text">
                                    {node.name}
                                </span>
                            </div>
                        </div>
                    </td>
                </tr>
            );
        }

        // Render children if expanded
        if (hasChildren && (isRoot || isExpanded)) {
            Array.from(node.children.values()).forEach(child => {
                nodes.push(...renderTreeNode(child, level + 1));
            });
        }

        return nodes;
    };

    // Get all sub-paths for a given path (including the path itself)
    const getSubPaths = (path: string): string[] => {
        return paths.filter(p => p === path || p.startsWith(path + ' > '));
    };

    const handleSaveChanges = async () => {
        if (!selectedPath) return;

        try {
            setSaving(true);

            // Get selected path and all sub-paths
            const targetPaths = getSubPaths(selectedPath);

            // Process each override
            for (const override of overrides) {
                if (override.permission === 'DENY') {
                    // Deny access to all paths
                    await permissionService.denyBulk({
                        userEmail: override.userEmail,
                        documentPaths: targetPaths
                    });
                } else {
                    // Grant READ access to all paths
                    await permissionService.grantBulk({
                        userEmail: override.userEmail,
                        documentPaths: targetPaths
                    });
                }
            }

            alert(`Permissions saved successfully for ${targetPaths.length} path(s)!`);
            await fetchPathDetail(selectedPath);
        } catch (err) {
            console.error('Failed to save changes:', err);
            alert('Failed to save changes. Please try again.');
        } finally {
            setSaving(false);
        }
    };

    const handleAddOverride = () => {
        if (!newOverride.userEmail) {
            alert('Please select a user');
            return;
        }

        // Check if user already has an override
        if (overrides.find(o => o.userEmail === newOverride.userEmail)) {
            alert('This user already has an override');
            return;
        }

        // Find the selected user
        const selectedUser = users.find(u => u.email === newOverride.userEmail);
        if (!selectedUser) {
            alert('User not found');
            return;
        }

        const override: Override = {
            id: `override-${Date.now()}`,
            userEmail: selectedUser.email,
            userName: selectedUser.name,
            permission: newOverride.permission
        };

        setOverrides([...overrides, override]);
        setNewOverride({ userEmail: '', permission: 'READ' });
        setShowAddOverride(false);
    };

    const handleRemoveOverride = async (id: string, userEmail: string) => {
        if (!selectedPath) return;

        if (!window.confirm('Are you sure you want to remove this permission override?')) {
            return;
        }

        try {
            // Revoke the permission from the backend
            await permissionService.revokeBulk({
                userEmail: userEmail,
                documentPaths: [selectedPath]
            });

            // Remove from local state
            setOverrides(overrides.filter(o => o.id !== id));

            // Refresh path detail
            await fetchPathDetail(selectedPath);
        } catch (err) {
            console.error('Failed to remove override:', err);
            alert('Failed to remove permission. Please try again.');
        }
    };

    const handleAddUserFromList = (user: User) => {
        if (!selectedPath) {
            alert('Please select a path first');
            return;
        }

        // Check if user already has an override
        if (overrides.find(o => o.userEmail === user.email)) {
            alert('This user already has an override for this path');
            return;
        }

        const override: Override = {
            id: `override-${Date.now()}`,
            userEmail: user.email,
            userName: user.name,
            permission: 'READ'
        };

        setOverrides([...overrides, override]);
    };

    return (
        <div className="animate-fade-in">
            <div className="mb-8">
                <h1 className="mb-2">Permission Manager</h1>
                <p className="text-secondary">Configure global access policies and inheritance</p>
            </div>

            <div className="grid grid-cols-4 gap-lg" style={{ minHeight: '600px' }}>
                {/* Sidebar / Tree View */}
                <div className="card p-0 overflow-hidden h-full flex flex-col" style={{ maxHeight: 'calc(100vh - 200px)' }}>
                    <div className="path-toolbar p-4 border-b bg-gradient-to-r from-gray-50 to-gray-100">
                        <h3 className="mb-3 text-base font-semibold">Structure</h3>
                        <div className="relative">
                            <div className="search-wrapper">
                                <span className="search-icon">🔍</span>
                                <input
                                    type="text"
                                    className="form-control path-search-input"
                                    placeholder="Search paths..."
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                            </div>
                        </div>
                    </div>

                    <div className="table-container path-table-container flex-1 overflow-y-auto">
                        <table className="table path-table">
                            <tbody>
                                {loading ? (
                                    <tr>
                                        <td className="text-center p-8 text-secondary">Loading paths...</td>
                                    </tr>
                                ) : filteredPaths.length === 0 ? (
                                    <tr>
                                        <td className="text-center p-8 text-secondary">No paths found</td>
                                    </tr>
                                ) : (
                                    renderTreeNode(treeRoot)
                                )}
                            </tbody>
                        </table>
                    </div>

                    <div className="path-footer p-3 border-t bg-gray-50 flex justify-between items-center">
                        <div className="path-stats text-xs text-secondary">
                            <span className="font-medium text-gray-700">{filteredPaths.length}</span>
                            <span className="ml-1">paths</span>
                        </div>
                        <div className="flex gap-2">
                            <button
                                onClick={() => {
                                    // Expand all
                                    const allPaths = new Set<string>();
                                    const collectPaths = (node: TreeNode) => {
                                        if (node.children.size > 0) {
                                            allPaths.add(node.fullPath);
                                            node.children.forEach(child => collectPaths(child));
                                        }
                                    };
                                    collectPaths(treeRoot);
                                    setExpandedPaths(allPaths);
                                }}
                                className="btn btn-secondary btn-sm path-control-btn"
                                style={{ fontSize: '11px', padding: '4px 8px' }}
                            >
                                Expand All
                            </button>
                            <button
                                onClick={() => setExpandedPaths(new Set())}
                                className="btn btn-secondary btn-sm path-control-btn"
                                style={{ fontSize: '11px', padding: '4px 8px' }}
                            >
                                Collapse All
                            </button>
                        </div>
                    </div>
                </div>

                {/* Main Content */}
                <div className="card col-span-2 h-full overflow-y-auto" style={{ maxHeight: 'calc(100vh - 200px)', gridColumn: 'span 2' }}>
                    <div className="flex justify-between items-center mb-6 border-b pb-4">
                        <div>
                            <h3 className="text-lg font-semibold m-0">Policy Settings</h3>
                            {selectedPath && (
                                <>
                                    <p className="text-sm text-secondary mt-1 m-0">{selectedPath}</p>
                                    <p className="text-xs text-blue-600 mt-1 m-0">
                                        {getSubPaths(selectedPath).length} path(s) including sub-directories
                                    </p>
                                </>
                            )}
                        </div>
                        <div className="flex gap-2">
                            {selectedPath && (
                                <Link
                                    to={`/admin/permissions/path/detail?path=${encodeURIComponent(selectedPath)}`}
                                    className="btn btn-secondary btn-sm"
                                >
                                    View Detail
                                </Link>
                            )}
                            <button 
                                className="btn btn-primary btn-sm"
                                onClick={handleSaveChanges}
                                disabled={saving || !selectedPath}
                            >
                                {saving ? 'Saving...' : 'Save Changes'}
                            </button>
                        </div>
                    </div>

                    {!selectedPath ? (
                        <div className="text-center text-secondary p-8">
                            Select a path from the structure to configure permissions
                        </div>
                    ) : (
                        <>
                            <div className="grid grid-cols-2 gap-lg mb-6">
                                <div className="card p-4 bg-gray-50">
                                    <div className="flex justify-between items-start mb-1">
                                        <div className="text-sm text-secondary">Documents</div>
                                        {selectedPath && pathDetail && pathDetail.totalDocuments > 0 && (
                                            <Link
                                                to={`/admin/permissions/path/detail?path=${encodeURIComponent(selectedPath)}`}
                                                className="text-xs text-primary hover:underline"
                                            >
                                                View Detail →
                                            </Link>
                                        )}
                                    </div>
                                    <div className="text-2xl font-bold text-main">
                                        {pathDetail?.totalDocuments ?? '-'}
                                    </div>
                                </div>
                                <div className="card p-4 bg-gray-50">
                                    <div className="flex justify-between items-start mb-1">
                                        <div className="text-sm text-secondary">Users with Access</div>
                                        {selectedPath && pathDetail && pathDetail.totalUsers > 0 && (
                                            <Link
                                                to={`/admin/permissions/path/detail?path=${encodeURIComponent(selectedPath)}`}
                                                className="text-xs text-primary hover:underline"
                                            >
                                                View Detail →
                                            </Link>
                                        )}
                                    </div>
                                    <div className="text-2xl font-bold text-main">
                                        {pathDetail?.totalUsers ?? '-'}
                                    </div>
                                </div>
                            </div>

                            <div className="input-group max-w-md mb-6">
                                <label className="input-label">Default Access Level</label>
                                <select
                                    className="form-control"
                                    value={defaultAccess}
                                    onChange={(e) => setDefaultAccess(e.target.value as typeof defaultAccess)}
                                >
                                    <option value="READ">Read</option>
                                    <option value="DENY">Deny Access</option>
                                </select>
                            </div>

                            <div className="input-group mb-6">
                                <label className="input-label">Inheritance</label>
                                <div className="p-4 border border-blue-200 rounded-lg bg-blue-50">
                                    <div className="font-medium text-sm text-blue-900">Auto-apply to sub-directories</div>
                                    <div className="text-xs text-blue-700 mt-1">
                                        Permissions will automatically apply to all {getSubPaths(selectedPath).length - 1} sub-paths under this directory
                                    </div>
                                </div>
                            </div>

                            <div className="mt-8">
                                <div className="flex justify-between items-center mb-4">
                                    <h4 className="text-base font-semibold m-0">Explicit Overrides</h4>
                                    <button 
                                        className="btn btn-secondary btn-sm"
                                        onClick={() => setShowAddOverride(!showAddOverride)}
                                    >
                                        {showAddOverride ? 'Cancel' : '+ Add Override'}
                                    </button>
                                </div>

                                {showAddOverride && (
                                    <div className="card p-4 mb-4 bg-gray-50 border border-blue-200">
                                        <div className="grid grid-cols-2 gap-md mb-3">
                                            <div>
                                                <label className="input-label text-sm">Select User</label>
                                                <select
                                                    className="form-control"
                                                    value={newOverride.userEmail}
                                                    onChange={(e) => setNewOverride({ ...newOverride, userEmail: e.target.value })}
                                                >
                                                    <option value="">-- Select a user --</option>
                                                    {users
                                                        .filter(user => !overrides.find(o => o.userEmail === user.email))
                                                        .map(user => (
                                                            <option key={user.email} value={user.email}>
                                                                {user.name} ({user.email})
                                                            </option>
                                                        ))
                                                    }
                                                </select>
                                            </div>
                                            <div>
                                                <label className="input-label text-sm">Permission</label>
                                                <select
                                                    className="form-control"
                                                    value={newOverride.permission}
                                                    onChange={(e) => setNewOverride({ ...newOverride, permission: e.target.value as typeof newOverride.permission })}
                                                >
                                                    <option value="READ">Read</option>
                                                    <option value="DENY">Deny Access</option>
                                                </select>
                                            </div>
                                        </div>
                                        <button
                                            className="btn btn-primary btn-sm"
                                            onClick={handleAddOverride}
                                        >
                                            Add Override
                                        </button>
                                    </div>
                                )}

                                <div className="table-container">
                                    <table className="table">
                                        <thead>
                                            <tr>
                                                <th>User</th>
                                                <th>Email</th>
                                                <th>Permission</th>
                                                <th>Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {overrides.length === 0 ? (
                                                <tr>
                                                    <td colSpan={4} className="text-center text-secondary p-4">
                                                        No overrides configured
                                                    </td>
                                                </tr>
                                            ) : (
                                                overrides.map(override => (
                                                    <tr key={override.id}>
                                                        <td className="font-medium">{override.userName}</td>
                                                        <td className="text-sm text-secondary">{override.userEmail}</td>
                                                        <td>
                                                            <select
                                                                className="form-control form-control-sm"
                                                                value={override.permission}
                                                                onChange={(e) => {
                                                                    const newPermission = e.target.value as typeof override.permission;
                                                                    setOverrides(overrides.map(o =>
                                                                        o.id === override.id
                                                                            ? { ...o, permission: newPermission }
                                                                            : o
                                                                    ));
                                                                }}
                                                                style={{ minWidth: '140px' }}
                                                            >
                                                                <option value="READ">Read</option>
                                                                <option value="DENY">Deny Access</option>
                                                            </select>
                                                        </td>
                                                        <td>
                                                            <button
                                                                className="btn btn-danger btn-sm"
                                                                onClick={() => handleRemoveOverride(override.id, override.userEmail)}
                                                            >
                                                                Remove
                                                            </button>
                                                        </td>
                                                    </tr>
                                                ))
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </>
                    )}
                </div>

                {/* User List Panel */}
                <div className="card p-0 overflow-hidden h-full flex flex-col" style={{ maxHeight: 'calc(100vh - 200px)' }}>
                    <div className="p-4 border-b bg-gradient-to-r from-gray-50 to-gray-100">
                        <h3 className="mb-3 text-base font-semibold">Users</h3>
                        <div className="relative">
                            <div className="search-wrapper">
                                <span className="search-icon">🔍</span>
                                <input
                                    type="text"
                                    className="form-control path-search-input"
                                    placeholder="Search users..."
                                    value={userSearchTerm}
                                    onChange={(e) => setUserSearchTerm(e.target.value)}
                                />
                            </div>
                        </div>
                    </div>

                    <div className="flex-1 overflow-y-auto">
                        {loading ? (
                            <div className="text-center p-8 text-secondary">Loading users...</div>
                        ) : filteredUsers.length === 0 ? (
                            <div className="text-center p-8 text-secondary">No users found</div>
                        ) : (
                            <div className="divide-y">
                                {filteredUsers.map(user => {
                                    const hasOverride = overrides.find(o => o.userEmail === user.email);
                                    return (
                                        <div
                                            key={user.email}
                                            className={`p-3 hover:bg-gray-50 cursor-pointer transition-colors ${
                                                hasOverride ? 'bg-blue-50' : ''
                                            }`}
                                            onClick={() => handleAddUserFromList(user)}
                                            title={hasOverride ? 'Already has permission' : 'Click to add permission'}
                                        >
                                            <div className="flex items-center gap-3">
                                                <div className="flex-shrink-0">
                                                    <div className="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center text-sm font-medium">
                                                        {user.name.charAt(0).toUpperCase()}
                                                    </div>
                                                </div>
                                                <div className="flex-1 min-w-0">
                                                    <div className="font-medium text-sm text-gray-900 truncate">
                                                        {user.name}
                                                    </div>
                                                    <div className="text-xs text-gray-500 truncate">
                                                        {user.email}
                                                    </div>
                                                </div>
                                                {hasOverride && (
                                                    <div className="flex-shrink-0">
                                                        <span className="text-xs text-blue-600">✓</span>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>

                    <div className="p-3 border-t bg-gray-50">
                        <div className="text-xs text-secondary text-center">
                            <span className="font-medium text-gray-700">{filteredUsers.length}</span>
                            <span className="ml-1">users</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PermissionManagementPage;
