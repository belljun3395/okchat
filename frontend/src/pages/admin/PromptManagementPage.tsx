import React, { useState, useEffect, useCallback } from 'react';
import { promptService } from '../../services/prompt.service';
import type { PromptResponse } from '../../types/prompt';

interface ApiError {
    response?: {
        status: number;
        data?: {
            message?: string;
        };
    };
}

const PromptManagementPage: React.FC = () => {
    const [promptType, setPromptType] = useState<string>('system');
    const [availableTypes, setAvailableTypes] = useState<string[]>([]);
    const [isNewTypeMode, setIsNewTypeMode] = useState<boolean>(false);
    const [newTypeName, setNewTypeName] = useState<string>('');

    const [currentCodeTye, setCurrentCodeType] = useState<string>('system');
    const [content, setContent] = useState<string>('');
    const [versions, setVersions] = useState<PromptResponse[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>('');
    const [successMessage, setSuccessMessage] = useState<string>('');
    const [isCreating, setIsCreating] = useState<boolean>(false);

    const fetchPromptTypes = useCallback(async () => {
        try {
            const response = await promptService.getAllTypes();
            setAvailableTypes(response.data);
            if (response.data.length > 0 && !isNewTypeMode) {
                // If types exist and we are not creating a new one, select the first one by default if not set
                // But we probably want to keep the current selection if valid
            }
        } catch (err) {
            console.error('Failed to fetch prompt types:', err);
        }
    }, [isNewTypeMode]);

    const fetchPromptData = useCallback(async (type: string) => {
        if (!type) return;
        
        setLoading(true);
        setError('');
        setSuccessMessage('');
        try {
            // Try to fetch all versions first to see if it exists
            const versionResponse = await promptService.getAllVersions(type);
            setVersions(versionResponse.data.sort((a, b) => b.version - a.version));
            
            // Get the latest version content
            const latest = versionResponse.data.find(v => v.version === Math.max(...versionResponse.data.map(i => i.version)));
            if (latest) {
                setContent(latest.content);
                setIsCreating(false);
            } else {
                 // Should ideally not happen if versions exist, but handle just in case
                 setIsCreating(true);
                 setContent('');
            }
            setCurrentCodeType(type);
        } catch (err: unknown) {
            // 404 means it doesn't exist yet
            const apiError = err as ApiError;
            if (apiError.response && apiError.response.status === 404) {
               setVersions([]);
               setContent('');
               setIsCreating(true);
               setCurrentCodeType(type);
               // Only show error if we are not in "New Type Mode" (which implies creation)
               if (!isNewTypeMode) {
                   setError(`Prompt type '${type}' not found. You can create it below.`);
               }
            } else {
                console.error('Failed to fetch prompt data:', err);
                setError('Failed to load prompt data. Please try again.');
            }
        } finally {
            setLoading(false);
        }
    }, [isNewTypeMode]);

    // Initial load
    useEffect(() => {
        fetchPromptTypes().then(() => {
             // After fetching types, if we have types, load the first one? 
             // Or keep 'system' default.
        });
        fetchPromptData('system');
    }, [fetchPromptTypes, fetchPromptData]);

    const handleTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const val = e.target.value;
        if (val === '__NEW__') {
            setIsNewTypeMode(true);
            setPromptType('');
            setVersions([]);
            setContent('');
            setIsCreating(true);
            setCurrentCodeType(''); // Clear current
        } else {
            setIsNewTypeMode(false);
            setPromptType(val);
            fetchPromptData(val);
        }
    };

    const handleNewTypeSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (newTypeName.trim()) {
            setPromptType(newTypeName.trim());
            setCurrentCodeType(newTypeName.trim());
            setIsCreating(true);
            setContent('');
            setVersions([]);
            // effectively we are ready to create
        }
    };



    const handleSave = async () => {
        if (!content.trim()) {
            setError('Prompt content cannot be empty.');
            return;
        }

        setLoading(true);
        setError('');
        setSuccessMessage('');

        try {
            if (isCreating) {
                await promptService.createPrompt({ type: currentCodeTye, content });
                setSuccessMessage(`Successfully created prompt '${currentCodeTye}'.`);
                setIsCreating(false);
                setNewTypeName('');
                setIsNewTypeMode(false);
                // Refresh types list as we added a new one
                fetchPromptTypes(); 
            } else {
                await promptService.updatePrompt(currentCodeTye, { content });
                setSuccessMessage(`Successfully updated prompt '${currentCodeTye}'.`);
            }
            // Refresh data
            await fetchPromptData(currentCodeTye);
        } catch (err: unknown) {
            console.error('Failed to save prompt:', err);
            const apiError = err as ApiError;
            setError(apiError.response?.data?.message || 'Failed to save prompt.');
        } finally {
            setLoading(false);
        }
    };

    const handleDeactivate = async (version: number) => {
        if (!window.confirm(`Are you sure you want to deactivate version ${version}?`)) return;

        setLoading(true);
        try {
            await promptService.deactivatePrompt(currentCodeTye, version);
            setSuccessMessage(`Version ${version} deactivated.`);
            fetchPromptData(currentCodeTye);
        } catch (err: unknown) {
            console.error('Failed to deactivate prompt:', err);
            setError('Failed to deactivate prompt version.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="animate-fade-in">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="mb-2">Prompt Management</h1>
                    <p className="text-secondary">Manage AI prompts and their versions</p>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Left Column: Search & Editor */}
                <div className="lg:col-span-2 space-y-6">
                    {/* Search Card */}
                    <div className="card p-6">
                        <h2 className="text-lg font-semibold mb-4">Select Prompt Type</h2>
                        
                        {!isNewTypeMode ? (
                            <div className="flex gap-4">
                                <div className="flex-1">
                                    <select 
                                        className="form-control w-full"
                                        value={promptType}
                                        onChange={handleTypeChange}
                                    >
                                        <option value="" disabled>Select a prompt type...</option>
                                        {availableTypes.map(type => (
                                            <option key={type} value={type}>{type}</option>
                                        ))}
                                        <option value="__NEW__">+ Create New Type...</option>
                                    </select>
                                </div>
                                <button 
                                    className="btn btn-secondary" 
                                    onClick={() => fetchPromptData(promptType)}
                                    disabled={loading || !promptType}
                                >
                                    Refresh
                                </button>
                            </div>
                        ) : (
                            <form onSubmit={handleNewTypeSubmit} className="flex gap-4 animate-fade-in">
                                <div className="flex-1">
                                    <input
                                        type="text"
                                        className="form-control w-full"
                                        placeholder="Enter new prompt type (e.g., summary_bot)"
                                        value={newTypeName}
                                        onChange={(e) => setNewTypeName(e.target.value)}
                                        autoFocus
                                    />
                                </div>
                                <button type="submit" className="btn btn-primary" disabled={!newTypeName.trim()}>
                                    Start Creating
                                </button>
                                <button 
                                    type="button" 
                                    className="btn btn-secondary"
                                    onClick={() => {
                                        setIsNewTypeMode(false);
                                        setNewTypeName('');
                                        // Reset to first available or 'system'
                                        if (availableTypes.length > 0) {
                                             setPromptType(availableTypes[0]);
                                             fetchPromptData(availableTypes[0]);
                                        }
                                    }}
                                >
                                    Cancel
                                </button>
                            </form>
                        )}
                    </div>

                    {/* Editor Card */}
                    <div className="card p-6">
                        <div className="flex justify-between items-center mb-4">
                            <h2 className="text-lg font-semibold">
                                {isCreating ? `Create '${currentCodeTye}'` : `Edit '${currentCodeTye}'`}
                            </h2>
                            {!isCreating && versions.length > 0 && (
                                <span className="badge badge-info">
                                    Latest Version: {versions[0].version}
                                </span>
                            )}
                        </div>

                        {error && <div className="p-4 mb-4 bg-red-50 text-red-600 rounded-md border border-red-100">{error}</div>}
                        {successMessage && <div className="p-4 mb-4 bg-green-50 text-green-600 rounded-md border border-green-100">{successMessage}</div>}

                        <div className="mb-6">
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Prompt Content
                            </label>
                            <textarea
                                className="form-control w-full font-mono text-sm"
                                rows={15}
                                value={content}
                                onChange={(e) => setContent(e.target.value)}
                                placeholder="Enter prompt content here..."
                            />
                        </div>

                        <div className="flex justify-end">
                            <button 
                                onClick={handleSave} 
                                className="btn btn-primary"
                                disabled={loading}
                            >
                                {isCreating ? 'Create Prompt' : 'Update Prompt'}
                            </button>
                        </div>
                    </div>
                </div>

                {/* Right Column: Version History */}
                <div className="lg:col-span-1">
                    <div className="card p-0 overflow-hidden">
                        <div className="p-4 border-b bg-gray-50">
                            <h2 className="text-lg font-semibold">Version History</h2>
                        </div>
                        <div className="max-h-[800px] overflow-y-auto">
                            {versions.length === 0 ? (
                                <div className="p-8 text-center text-secondary">
                                    No versions found.
                                </div>
                            ) : (
                                <ul className="divide-y divide-gray-100">
                                    {versions.map((v) => (
                                        <li key={v.version} className={`p-4 hover:bg-gray-50 transition-colors ${!v.isActive ? 'opacity-50' : ''}`}>
                                            <div className="flex justify-between items-start mb-2">
                                                <div>
                                                    <span className="font-medium text-gray-900">Version {v.version}</span>
                                                    {!v.isActive && <span className="ml-2 text-xs text-red-500 font-medium">(Inactive)</span>}
                                                </div>
                                                {v.isActive && (
                                                    <button 
                                                        onClick={() => handleDeactivate(v.version)}
                                                        className="text-xs text-red-600 hover:text-red-800"
                                                        title="Deactivate this version"
                                                    >
                                                        Deactivate
                                                    </button>
                                                )}
                                            </div>
                                            <div className="text-xs text-gray-500 line-clamp-3 font-mono bg-gray-50 p-2 rounded border border-gray-100">
                                                {v.content}
                                            </div>
                                            <div className="mt-2 flex justify-between items-center text-xs text-gray-400">
                                                <button 
                                                    className="text-primary hover:text-primary-dark"
                                                    onClick={() => setContent(v.content)}
                                                >
                                                    Load this version
                                                </button>
                                            </div>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PromptManagementPage;
