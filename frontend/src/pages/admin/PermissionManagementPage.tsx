import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import type { PathDetailResponse } from '../../types';

interface TreeNode {
    name: string;
    fullPath: string;
    children: Map<string, TreeNode>;
    isExpanded?: boolean;
}

interface Override {
    id: string;
    userEmail: string;
    userName: string;
    permission: 'Read Only' | 'Write' | 'No Access';
}

const PermissionManagementPage: React.FC = () => {
    const [paths, setPaths] = useState<string[]>([]);
    const [selectedPath, setSelectedPath] = useState<string>('');
    const [pathDetail, setPathDetail] = useState<PathDetailResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [expandedPaths, setExpandedPaths] = useState<Set<string>>(new Set());
    
    const [defaultAccess, setDefaultAccess] = useState<'Read Only' | 'Write' | 'No Access'>('Read Only');
    const [inherit, setInherit] = useState(true);
    const [overrides, setOverrides] = useState<Override[]>([]);
    const [showAddOverride, setShowAddOverride] = useState(false);
    const [newOverride, setNewOverride] = useState({ userEmail: '', permission: 'Read Only' as const });

    const fetchPaths = useCallback(async () => {
        try {
            setLoading(true);
            const response = await axios.get<string[]>('/api/admin/permissions/paths');
            setPaths(response.data);
            if (response.data.length > 0 && !selectedPath) {
                setSelectedPath(response.data[0]);
            }
        } catch (err) {
            console.error('Failed to fetch paths:', err);
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
            const response = await axios.get<PathDetailResponse>(`/api/admin/permissions/path/detail?path=${encodeURIComponent(path)}`);
            setPathDetail(response.data);
            // Initialize overrides from users with access
            const initialOverrides: Override[] = response.data.usersWithAccess.map((user, idx) => ({
                id: `override-${idx}`,
                userEmail: user.email,
                userName: user.name,
                permission: 'Read Only' // Default, could be enhanced to fetch actual permission type
            }));
            setOverrides(initialOverrides);
        } catch (err) {
            console.error('Failed to fetch path detail:', err);
        }
    };

    // Build tree structure from paths
    const treeRoot = useMemo(() => {
        const root: TreeNode = { name: 'Root', fullPath: '', children: new Map() };
        
        paths.forEach(path => {
            const parts = path.split('/').filter(p => p);
            let current = root;
            
            parts.forEach((part, index) => {
                if (!current.children.has(part)) {
                    const fullPath = '/' + parts.slice(0, index + 1).join('/');
                    current.children.set(part, {
                        name: part,
                        fullPath,
                        children: new Map(),
                        isExpanded: expandedPaths.has(fullPath)
                    });
                }
                current = current.children.get(part)!;
            });
        });
        
        return root;
    }, [paths, expandedPaths]);

    // Filter paths based on search term
    const filteredPaths = useMemo(() => {
        if (!searchTerm) return paths;
        const term = searchTerm.toLowerCase();
        return paths.filter(path => path.toLowerCase().includes(term));
    }, [paths, searchTerm]);

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

    const renderTreeNode = (node: TreeNode, level: number = 0): React.ReactNode => {
        const hasChildren = node.children.size > 0;
        const isExpanded = expandedPaths.has(node.fullPath);
        const isSelected = selectedPath === node.fullPath;
        const matchesSearch = !searchTerm || node.fullPath.toLowerCase().includes(searchTerm.toLowerCase()) ||
            Array.from(node.children.keys()).some(child => child.toLowerCase().includes(searchTerm.toLowerCase()));

        if (!matchesSearch && searchTerm) return null;

        const indentSize = level * 20;
        const isRoot = level === 0;

        return (
            <div key={node.fullPath || 'root'} className="tree-node-wrapper">
                <div
                    className={`tree-node flex items-center gap-2 py-2.5 px-3 rounded-md cursor-pointer transition-all ${
                        isSelected 
                            ? 'bg-blue-50 text-blue-700 border-l-4 border-blue-500 shadow-sm font-semibold' 
                            : 'hover:bg-gray-50 text-gray-700 border-l-4 border-transparent'
                    }`}
                    style={{ 
                        paddingLeft: `${indentSize + 12}px`,
                        marginBottom: '1px'
                    }}
                    onClick={() => {
                        if (node.fullPath) {
                            setSelectedPath(node.fullPath);
                        }
                        if (hasChildren) {
                            toggleExpand(node.fullPath);
                        }
                    }}
                    title={node.fullPath || node.name}
                >
                    {/* Expand/Collapse icon */}
                    <div className="flex-shrink-0 tree-icon-wrapper" style={{ width: '20px', textAlign: 'center' }}>
                        {hasChildren ? (
                            <span className="tree-icon text-gray-600" style={{ 
                                fontSize: '11px',
                                fontWeight: 'bold',
                                display: 'inline-block',
                                transition: 'transform 0.2s',
                                transform: isExpanded ? 'rotate(0deg)' : 'rotate(-90deg)'
                            }}>
                                ▼
                            </span>
                        ) : (
                            <span className="tree-icon text-gray-400" style={{ fontSize: '6px' }}>
                                ●
                            </span>
                        )}
                    </div>
                    
                    {/* Node name */}
                    <span 
                        className={`tree-node-name flex-1 ${
                            isSelected ? 'font-semibold' : 'font-normal'
                        }`}
                        style={{ 
                            fontSize: isRoot ? '14px' : level === 1 ? '13px' : '12px',
                            lineHeight: '1.5',
                            color: isSelected ? '#1e40af' : level > 2 ? '#6b7280' : '#374151'
                        }}
                    >
                        {node.name}
                    </span>
                </div>
                
                {/* Children */}
                {hasChildren && isExpanded && (
                    <div className="tree-children" style={{ position: 'relative' }}>
                        {Array.from(node.children.values()).map(child => 
                            renderTreeNode(child, level + 1)
                        )}
                    </div>
                )}
            </div>
        );
    };

    const handleSaveChanges = async () => {
        if (!selectedPath) return;
        
        try {
            setSaving(true);
            // TODO: Implement actual save API call
            // await axios.put(`/api/admin/permissions/path/${encodeURIComponent(selectedPath)}`, {
            //     defaultAccess,
            //     inherit,
            //     overrides
            // });
            
            // Simulate API call
            await new Promise(resolve => setTimeout(resolve, 500));
            alert('Changes saved successfully!');
        } catch (err) {
            console.error('Failed to save changes:', err);
            alert('Failed to save changes. Please try again.');
        } finally {
            setSaving(false);
        }
    };

    const handleAddOverride = () => {
        if (!newOverride.userEmail) {
            alert('Please enter a user email');
            return;
        }
        
        // TODO: Validate user exists
        const override: Override = {
            id: `override-${Date.now()}`,
            userEmail: newOverride.userEmail,
            userName: newOverride.userEmail.split('@')[0], // Temporary
            permission: newOverride.permission
        };
        
        setOverrides([...overrides, override]);
        setNewOverride({ userEmail: '', permission: 'Read Only' });
        setShowAddOverride(false);
    };

    const handleRemoveOverride = (id: string) => {
        setOverrides(overrides.filter(o => o.id !== id));
    };

    return (
        <div className="animate-fade-in">
            <div className="mb-8">
                <h1 className="mb-2">Permission Manager</h1>
                <p className="text-secondary">Configure global access policies and inheritance</p>
            </div>

            <div className="grid grid-cols-3 gap-lg" style={{ minHeight: '600px' }}>
                {/* Sidebar / Tree View */}
                <div className="card h-full flex flex-col" style={{ maxHeight: 'calc(100vh - 200px)' }}>
                    <h3 className="mb-4 text-base font-semibold">Structure</h3>
                    <div className="input-group mb-4">
                        <input
                            type="text"
                            className="form-control"
                            placeholder="Search hierarchy..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    
                    {loading ? (
                        <div className="text-center text-secondary p-4">Loading paths...</div>
                    ) : (
                        <div className="tree-container flex flex-col overflow-y-auto flex-1" style={{ 
                            padding: '8px 4px',
                            backgroundColor: '#ffffff'
                        }}>
                            {renderTreeNode(treeRoot)}
                            {filteredPaths.length === 0 && (
                                <div className="text-center text-secondary p-4 text-sm">
                                    No paths found
                                </div>
                            )}
                        </div>
                    )}
                </div>

                {/* Main Content */}
                <div className="card col-span-2 h-full overflow-y-auto" style={{ maxHeight: 'calc(100vh - 200px)' }}>
                    <div className="flex justify-between items-center mb-6 border-b pb-4">
                        <div>
                            <h3 className="text-lg font-semibold m-0">Policy Settings</h3>
                            {selectedPath && (
                                <p className="text-sm text-secondary mt-1 m-0">{selectedPath}</p>
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
                                    <option>Read Only</option>
                                    <option>Write</option>
                                    <option>No Access</option>
                                </select>
                            </div>

                            <div className="input-group mb-6">
                                <label className="input-label">Inheritance</label>
                                <label className="flex items-center gap-sm p-4 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50 transition-colors">
                                    <input
                                        type="checkbox"
                                        checked={inherit}
                                        onChange={(e) => setInherit(e.target.checked)}
                                        className="w-4 h-4 text-primary rounded border-gray-300 focus:ring-primary"
                                    />
                                    <div>
                                        <div className="font-medium text-sm text-main">Inherit from parent</div>
                                        <div className="text-xs text-secondary">Permissions will cascade down to sub-directories</div>
                                    </div>
                                </label>
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
                                                <label className="input-label text-sm">User Email</label>
                                                <input
                                                    type="email"
                                                    className="form-control"
                                                    placeholder="user@example.com"
                                                    value={newOverride.userEmail}
                                                    onChange={(e) => setNewOverride({ ...newOverride, userEmail: e.target.value })}
                                                />
                                            </div>
                                            <div>
                                                <label className="input-label text-sm">Permission</label>
                                                <select
                                                    className="form-control"
                                                    value={newOverride.permission}
                                                    onChange={(e) => setNewOverride({ ...newOverride, permission: e.target.value as typeof newOverride.permission })}
                                                >
                                                    <option>Read Only</option>
                                                    <option>Write</option>
                                                    <option>No Access</option>
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
                                                            <span className={`badge ${
                                                                override.permission === 'Read Only' ? 'badge-info' :
                                                                override.permission === 'Write' ? 'badge-success' :
                                                                'badge-secondary'
                                                            }`}>
                                                                {override.permission}
                                                            </span>
                                                        </td>
                                                        <td>
                                                            <button
                                                                className="btn btn-danger btn-sm"
                                                                onClick={() => handleRemoveOverride(override.id)}
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
            </div>
        </div>
    );
};

export default PermissionManagementPage;
