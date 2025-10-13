// Build tree structure from flat path list
function buildTree(pathsList) {
    if (!pathsList || !Array.isArray(pathsList)) {
        console.error('buildTree received invalid paths:', pathsList);
        return {};
    }

    const tree = {};

    pathsList.forEach(path => {
        if (!path || typeof path !== 'string') {
            console.warn('Skipping invalid path:', path);
            return;
        }
        const parts = path.split(' > ').map(p => p.trim());
        let current = tree;

        parts.forEach((part, index) => {
            if (!current[part]) {
                current[part] = {
                    name: part,
                    fullPath: parts.slice(0, index + 1).join(' > '),
                    children: {},
                    isLeaf: index === pathsList.length - 1
                };
            }
            current = current[part].children;
        });
    });

    return tree;
}

// Render tree recursively
function renderTree(node, options = {}) {
    if (!node || typeof node !== 'object') {
        console.error('renderTree received invalid node:', node);
        return '';
    }
    const entries = Object.entries(node).sort((a, b) => a[0].localeCompare(b[0]));

    return entries.map(([key, value]) => {
        const hasChildren = Object.keys(value.children).length > 0;
        const directPermission = options.hasDirectPermission ? options.hasDirectPermission(value.fullPath) : false;
        const inheritedPermission = options.hasPermission ? (!directPermission && options.hasPermission(value.fullPath)) : false;
        const denied = options.isDenied ? options.isDenied(value.fullPath) : false;
        const hasAnyPermission = directPermission || inheritedPermission || denied;

        let isChecked = options.selectedFolders ? options.selectedFolders.has(value.fullPath) : false;
        let isIndeterminate = false;

        if (hasChildren && options.showCheckboxes && options.allPaths) {
            const descendantPaths = options.allPaths.filter(p => p.startsWith(value.fullPath + ' > ') || p === value.fullPath);
            const selectedDescendantPaths = descendantPaths.filter(p => options.selectedFolders.has(p));

            if (selectedDescendantPaths.length > 0 && selectedDescendantPaths.length < descendantPaths.length) {
                isIndeterminate = true;
                isChecked = false; // Indeterminate implies not fully checked
            } else if (selectedDescendantPaths.length === descendantPaths.length && descendantPaths.length > 0) {
                isChecked = true;
                isIndeterminate = false;
            } else {
                isChecked = false;
                isIndeterminate = false;
            }
        }

        let checkboxHtml = '';
        if (options.showCheckboxes) {
            checkboxHtml = `<input type="checkbox" class="node-checkbox folder-checkbox" data-path="${value.fullPath}" ${isChecked ? 'checked' : ''} ${isIndeterminate ? 'indeterminate' : ''}>`;
        } else if (options.showUserDetailCheckboxes) {
             checkboxHtml = `${hasAnyPermission ? `<input type="checkbox" class="node-checkbox" data-path="${value.fullPath}" data-is-direct="${directPermission || denied ? 'true' : 'false'}" ${isChecked ? 'checked' : ''}>` : '<span style="width: 18px; display: inline-block;"></span>'}`;
        }


        let permissionBadgeHtml = '';
        if (options.showPermissionBadges) {
            if (directPermission) {
                permissionBadgeHtml = '<span class="permission-badge">✓ Direct</span>';
            } else if (inheritedPermission && !denied) {
                permissionBadgeHtml = '<span class="permission-badge" style="background: #6b7280;">✓ Inherited</span>';
            } else if (denied) {
                permissionBadgeHtml = '<span class="permission-badge" style="background: #ef4444;">❌ Denied</span>';
            }
        }

        let actionButtonHtml = '';
        if (options.showActionButton) {
            actionButtonHtml = `<button class="tree-action" onclick="event.stopPropagation(); window.location.href='/admin/permissions/path/${encodeURIComponent(value.fullPath)}'">View Details</button>`;
        }

        return `
            <div class="tree-node" data-path="${value.fullPath}">
                <div class="tree-node-content">
                    <span class="expand-icon">${hasChildren ? '▶' : ''}</span>
                    ${checkboxHtml}
                    <span class="node-icon">${hasChildren ? '📁' : '📄'}</span>
                    <span class="node-label">${value.name}</span>
                    ${permissionBadgeHtml}
                    ${actionButtonHtml}
                </div>
                ${hasChildren ? `
                    <div class="tree-children">
                        ${renderTree(value.children, options)}
                    </div>
                ` : ''}
            </div>
        `;
    }).join('');
}