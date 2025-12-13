import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import { permissionService } from '../../services';
import { knowledgeBaseService } from '../../services/knowledgeBase.service';

interface TreeNode {
    name: string;
    fullPath: string;
    children: Map<string, TreeNode>;
    isLeaf: boolean;
}

/**
 * PathManagementPage Component
 * 
 * Displays a list of all document paths in the system as a tree structure.
 * 
 * API: GET /api/admin/permissions/paths (via PermissionController)
 */
const PathManagementPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const kbId = id ? parseInt(id, 10) : undefined;

    const [paths, setPaths] = useState<string[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [searchTerm, setSearchTerm] = useState('');
    const [expandedPaths, setExpandedPaths] = useState<Set<string>>(new Set());

    const [kbName, setKbName] = useState<string>('');

    const fetchKbDetails = useCallback(async () => {
        try {
            const response = await knowledgeBaseService.getAll();
            const kb = response.data.find(k => k.id === kbId);
            if (kb) {
                setKbName(kb.name);
            }
        } catch (err) {
            console.error('Failed to fetch KB details:', err);
        }
    }, [kbId]);

    // No auto-expand - user controls expansion manually

    const fetchPaths = useCallback(async () => {
        try {
            setLoading(true);
            const response = await permissionService.getAllPaths(kbId);
            setPaths(response.data);
            setError('');
        } catch (err) {
            console.error('Failed to fetch paths:', err);
            setError('Failed to load paths. Please try again later.');
        } finally {
            setLoading(false);
        }
    }, [kbId]);

    useEffect(() => {
        fetchPaths();
        if (kbId) {
            fetchKbDetails();
        }
    }, [fetchPaths, fetchKbDetails, kbId]);

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
                } else {
                    // Update isLeaf if this node appears as an intermediate node
                    const existingNode = current.children.get(part)!;
                    if (index < parts.length - 1) {
                        existingNode.isLeaf = false;
                    }
                }
                current = current.children.get(part)!;
            });
        });

        console.log('Tree Root:', root);
        console.log('First level children:', Array.from(root.children.keys()));

        // Log second level children
        root.children.forEach((child, key) => {
            console.log(`Children of "${key}":`, Array.from(child.children.keys()).slice(0, 10));
        });

        return root;
    }, [paths]);

    // Filter tree based on search term
    const shouldShowNode = (node: TreeNode): boolean => {
        if (!searchTerm) return true;
        const term = searchTerm.toLowerCase();
        return node.fullPath.toLowerCase().includes(term) ||
               node.name.toLowerCase().includes(term) ||
               Array.from(node.children.values()).some(child => shouldShowNode(child));
    };

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

    // Render tree node
    const renderTreeNode = (node: TreeNode, level: number = 0): React.ReactNode[] => {
        if (!shouldShowNode(node)) return [];

        const hasChildren = node.children.size > 0;
        const isExpanded = expandedPaths.has(node.fullPath);
        const indentSize = level * 24;
        const isRoot = level === 0;
        const isFirstLevel = level === 1;

        if (!isRoot && level <= 3) {
            console.log(`Rendering "${node.name}" at level ${level}, indentSize: ${indentSize}, paddingLeft: ${indentSize + 16}px`);
        }

        const nodes: React.ReactNode[] = [];

        // Render current node (skip root)
        if (!isRoot) {
            if (level <= 2) {
                console.log(`[RENDER] "${node.name.substring(0, 30)}..." level=${level}, indent=${indentSize + 16}px`);
            }
            nodes.push(
                <tr key={node.fullPath} className={`path-row path-row-level-${level} ${isFirstLevel ? 'path-row-first-level' : ''}`}>
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
                    <td className="path-actions-cell">
                        <Link
                            to={`/admin/permissions/path/detail?path=${encodeURIComponent(node.fullPath)}`}
                            className="path-action-link"
                        >
                            View Details
                        </Link>
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

    const filteredPaths = paths.filter(path =>
        path.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="animate-fade-in">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="mb-2">Manage Paths {kbName ? `(${kbName})` : (kbId ? `(KB #${kbId})` : '')}</h1>
                    <p className="text-secondary">View and manage document path permissions</p>
                </div>
            </div>

            <div className="card p-0 overflow-hidden path-management-card">
                <div className="path-toolbar p-4 border-b bg-gradient-to-r from-gray-50 to-gray-100 flex gap-md items-center">
                    <div className="relative flex-1 max-w-md">
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

                <div className="table-container path-table-container">
                    <table className="table path-table">
                        <thead>
                            <tr className="path-table-header">
                                <th style={{ width: '70%' }}>Path</th>
                                <th style={{ width: '30%' }}>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan={2} className="text-center p-8 text-secondary">Loading paths...</td>
                                </tr>
                            ) : error ? (
                                <tr>
                                    <td colSpan={2} className="text-center p-8 text-danger">{error}</td>
                                </tr>
                            ) : filteredPaths.length === 0 ? (
                                <tr>
                                    <td colSpan={2} className="text-center p-8 text-secondary">No paths found</td>
                                </tr>
                            ) : (
                                renderTreeNode(treeRoot)
                            )}
                        </tbody>
                    </table>
                </div>

                <div className="path-footer p-4 border-t bg-gray-50 flex justify-between items-center">
                    <div className="path-stats text-sm text-secondary">
                        <span className="font-medium text-gray-700">{filteredPaths.length}</span>
                        <span className="ml-1">paths found</span>
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
                        >
                            Expand All
                        </button>
                        <button
                            onClick={() => setExpandedPaths(new Set())}
                            className="btn btn-secondary btn-sm path-control-btn"
                        >
                            Collapse All
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PathManagementPage;
