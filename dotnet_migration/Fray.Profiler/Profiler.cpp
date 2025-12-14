#include "Profiler.h"
#include <iostream>

OnThreadEventCallback g_managedCallback = nullptr;

extern "C" void STDMETHODCALLTYPE SetManagedCallback(OnThreadEventCallback callback)
{
    std::cerr << "[Fray.Profiler] SetManagedCallback called." << std::endl;
    g_managedCallback = callback;
}

Profiler::Profiler() : _refCount(1), _pInfo(nullptr) {}

Profiler::~Profiler()
{
    if (_pInfo)
    {
        _pInfo->Release();
        _pInfo = nullptr;
    }
}

HRESULT STDMETHODCALLTYPE Profiler::QueryInterface(REFIID riid, void **ppvObject)
{
    if (ppvObject == nullptr) return E_POINTER;

    if (riid == IID_IUnknown)
    {
        *ppvObject = static_cast<IUnknown *>(static_cast<ICorProfilerCallback2 *>(this));
    }
    else if (riid == IID_ICorProfilerCallback2)
    {
        *ppvObject = static_cast<ICorProfilerCallback2 *>(this);
    }
    else if (riid == IID_ICorProfilerCallback)
    {
        *ppvObject = static_cast<ICorProfilerCallback *>(this);
    }
    else
    {
        *ppvObject = nullptr;
        return E_NOINTERFACE;
    }

    AddRef();
    return S_OK;
}

ULONG STDMETHODCALLTYPE Profiler::AddRef()
{
    return ++_refCount;
}

ULONG STDMETHODCALLTYPE Profiler::Release()
{
    int count = --_refCount;
    if (count == 0)
    {
        delete this;
    }
    return count;
}

HRESULT STDMETHODCALLTYPE Profiler::Initialize(IUnknown *pICorProfilerInfoUnk)
{
    std::cerr << "[Fray.Profiler] Initialize." << std::endl;
    HRESULT hr = pICorProfilerInfoUnk->QueryInterface(IID_ICorProfilerInfo, (void **)&_pInfo);
    if (FAILED(hr))
    {
        std::cerr << "[Fray.Profiler] Failed to get ICorProfilerInfo. HR=" << std::hex << hr << std::endl;
        return E_FAIL;
    }

    hr = _pInfo->SetEventMask(COR_PRF_MONITOR_THREADS | 0x20); // Threads | JITCompilation
    if (FAILED(hr))
    {
        std::cerr << "[Fray.Profiler] Failed to SetEventMask. HR=" << std::hex << hr << std::endl;
        return E_FAIL;
    }

    return S_OK;
}

HRESULT STDMETHODCALLTYPE Profiler::Shutdown()
{
    std::cerr << "[Fray.Profiler] Shutdown." << std::endl;
    return S_OK;
}

HRESULT STDMETHODCALLTYPE Profiler::ThreadCreated(ThreadID threadId)
{
    std::cerr << "[Fray.Profiler] ThreadCreated: " << std::hex << threadId << std::dec << std::endl;
    if (g_managedCallback)
    {
        g_managedCallback((unsigned long long)threadId, 1); // 1 = Created
    }
    return S_OK;
}

HRESULT STDMETHODCALLTYPE Profiler::ThreadDestroyed(ThreadID threadId)
{
    std::cerr << "[Fray.Profiler] ThreadDestroyed: " << std::hex << threadId << std::dec << std::endl;
    if (g_managedCallback)
    {
        g_managedCallback((unsigned long long)threadId, 2); // 2 = Destroyed
    }
    return S_OK;
}

// Stubs for remaining methods
HRESULT STDMETHODCALLTYPE Profiler::AppDomainCreationStarted(AppDomainID appDomainId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::AppDomainCreationFinished(AppDomainID appDomainId, HRESULT hrStatus) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::AppDomainShutdownStarted(AppDomainID appDomainId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::AppDomainShutdownFinished(AppDomainID appDomainId, HRESULT hrStatus) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::AssemblyLoadStarted(AssemblyID assemblyId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::AssemblyLoadFinished(AssemblyID assemblyId, HRESULT hrStatus) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::AssemblyUnloadStarted(AssemblyID assemblyId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::AssemblyUnloadFinished(AssemblyID assemblyId, HRESULT hrStatus) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ModuleLoadStarted(ModuleID moduleId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ModuleLoadFinished(ModuleID moduleId, HRESULT hrStatus) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ModuleUnloadStarted(ModuleID moduleId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ModuleUnloadFinished(ModuleID moduleId, HRESULT hrStatus) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ModuleAttachedToAssembly(ModuleID moduleId, AssemblyID assemblyId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ClassLoadStarted(ClassID classId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ClassLoadFinished(ClassID classId, HRESULT hrStatus) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ClassUnloadStarted(ClassID classId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ClassUnloadFinished(ClassID classId, HRESULT hrStatus) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::FunctionUnloadStarted(FunctionID functionId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::JITCompilationStarted(FunctionID functionId, BOOL fIsSafeToBlock) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::JITCompilationFinished(FunctionID functionId, HRESULT hrStatus, BOOL fIsSafeToBlock) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::JITCachedFunctionSearchStarted(FunctionID functionId, BOOL *pbUseCachedFunction) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::JITCachedFunctionSearchFinished(FunctionID functionId, HRESULT hrStatus) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::JITFunctionPitched(FunctionID functionId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::JITInlining(FunctionID callerId, FunctionID calleeId, BOOL *pfShouldInline) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ThreadAssignedToOSThread(ThreadID managedThreadId, DWORD osThreadId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RemotingClientInvocationStarted() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RemotingClientSendingMessage(GUID *pCookie, BOOL fIsAsync) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RemotingClientReceivingReply(GUID *pCookie, BOOL fIsAsync) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RemotingClientInvocationFinished() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RemotingServerReceivingMessage(GUID *pCookie, BOOL fIsAsync) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RemotingServerInvocationStarted() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RemotingServerInvocationReturned() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RemotingServerSendingReply(GUID *pCookie, BOOL fIsAsync) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::UnmanagedToManagedTransition(FunctionID functionId, COR_PRF_TRANSITION_REASON reason) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ManagedToUnmanagedTransition(FunctionID functionId, COR_PRF_TRANSITION_REASON reason) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RuntimeSuspendStarted(COR_PRF_SUSPEND_REASON suspendReason) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RuntimeSuspendFinished() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RuntimeSuspendAborted() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RuntimeThreadSuspended(ThreadID threadId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RuntimeThreadResumed(ThreadID threadId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::MovedReferences(ULONG cMovedObjectIDRanges, ObjectID oldObjectIDRangeStart[], ObjectID newObjectIDRangeStart[], ULONG cObjectIDRangeLength[]) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ObjectAllocated(ObjectID objectId, ClassID classId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ObjectsAllocatedByClass(ULONG cClassCount, ClassID classIds[], ULONG cObjects[]) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ObjectReferences(ObjectID objectId, ClassID classId, ULONG cObjectRefs, ObjectID objectRefIds[]) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RootReferences(ULONG cRootRefs, ObjectID rootRefIds[]) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionThrown(ObjectID thrownObjectId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionSearchFunctionEnter(FunctionID functionId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionSearchFunctionLeave() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionSearchFilterEnter(FunctionID functionId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionSearchFilterLeave() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionSearchCatcherFound(FunctionID functionId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionOSHandlerEnter(UINT_PTR __unused) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionOSHandlerLeave(UINT_PTR __unused) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionUnwindFunctionEnter(FunctionID functionId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionUnwindFunctionLeave() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionUnwindFinallyEnter(FunctionID functionId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionUnwindFinallyLeave() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionCatcherEnter(FunctionID functionId, ObjectID objectId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionCatcherLeave() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::COMClassicVTableCreated(ClassID wrappedClassId, REFGUID implementedIID, void *pVTable, ULONG cSlots) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::COMClassicVTableDestroyed(ClassID wrappedClassId, REFGUID implementedIID, void *pVTable) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionCLRCatcherFound() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ExceptionCLRCatcherExecute() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ThreadNameChanged(ThreadID threadId, ULONG cchName, WCHAR name[]) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::GarbageCollectionStarted(int cGenerations, BOOL generationCollected[], COR_PRF_GC_REASON reason) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::GarbageCollectionFinished() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::FinalizeableObjectQueued(DWORD finalizerFlags, ObjectID objectID) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::RootReferences2(ULONG cRootRefs, ObjectID rootRefIds[], COR_PRF_GC_ROOT_KIND rootKinds[], COR_PRF_GC_ROOT_FLAGS rootFlags[], UINT_PTR rootIds[]) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::HandleCreated(UINT_PTR handleId, ObjectID initialObjectId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::HandleDestroyed(UINT_PTR handleId) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::InitializeForAttach(IUnknown *pCorProfilerInfoUnk, void *pvClientData, UINT cbClientData) { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ProfilerAttachFinished() { return S_OK; }
HRESULT STDMETHODCALLTYPE Profiler::ProfilerDetachSucceeded() { return S_OK; }
