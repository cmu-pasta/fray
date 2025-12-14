#pragma once

#include "headers/ole2.h"
#include <atomic>

// GUIDs
static const GUID IID_ICorProfilerCallback =
{ 0x176FBED1, 0xA55C, 0x4796, { 0x98, 0xCA, 0xA9, 0xDA, 0x0E, 0xF8, 0x83, 0xE7 } };

static const GUID IID_ICorProfilerCallback2 =
{ 0x8A8CC829, 0xCCF2, 0x49fe, { 0xBB, 0xAE, 0x0F, 0x02, 0x22, 0x28, 0x07, 0x1A } };

static const GUID IID_ICorProfilerInfo =
{ 0x28B5557D, 0x3F3F, 0x48b4, { 0x90, 0xB2, 0x5F, 0x9E, 0xEA, 0x2F, 0x6C, 0x48 } };

// {846F66E7-53E3-4D88-B394-F70C15797305}
static const GUID CLSID_FrayProfiler =
{ 0x846f66e7, 0x53e3, 0x4d88, { 0xb3, 0x94, 0xf7, 0xc, 0x15, 0x79, 0x73, 0x5 } };

// Basic types
typedef uintptr_t ProcessID;
typedef uintptr_t AssemblyID;
typedef uintptr_t AppDomainID;
typedef uintptr_t ModuleID;
typedef uintptr_t ClassID;
typedef uintptr_t ThreadID;
typedef uintptr_t FunctionID;
typedef uintptr_t ObjectID;
typedef uintptr_t GCHandleID;
typedef unsigned int mdToken;
typedef unsigned int mdTypeDef;
typedef unsigned int mdMethodDef;
typedef int CorElementType;
typedef int COR_PRF_TRANSITION_REASON;
typedef int COR_PRF_SUSPEND_REASON;
typedef int COR_PRF_GC_REASON;
typedef int COR_PRF_GC_ROOT_KIND;
typedef int COR_PRF_GC_ROOT_FLAGS;

struct ICorProfilerInfo : public IUnknown
{
    virtual HRESULT STDMETHODCALLTYPE GetClassFromObject(
        /* [in] */ ObjectID objectId,
        /* [out] */ ClassID *pClassId) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetClassFromToken(
        /* [in] */ ModuleID moduleId,
        /* [in] */ mdTypeDef typeDef,
        /* [out] */ ClassID *pClassId) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetCodeInfo(
        /* [in] */ FunctionID functionId,
        /* [out] */ BYTE *pStart, // LPCBYTE
        /* [out] */ ULONG *pcSize) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetEventMask(
        /* [out] */ DWORD *pdwEvents) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetFunctionFromIP(
        /* [in] */ BYTE *ip,
        /* [out] */ FunctionID *pFunctionId) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetFunctionFromToken(
        /* [in] */ ModuleID moduleId,
        /* [in] */ mdToken token,
        /* [out] */ FunctionID *pFunctionId) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetHandleFromThread(
        /* [in] */ ThreadID threadId,
        /* [out] */ HANDLE *phThread) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetObjectSize(
        /* [in] */ ObjectID objectId,
        /* [out] */ ULONG *pcSize) = 0;

    virtual HRESULT STDMETHODCALLTYPE IsArrayClass(
        /* [in] */ ClassID classId,
        /* [out] */ CorElementType *pBaseElemType,
        /* [out] */ ClassID *pBaseClassId,
        /* [out] */ ULONG *pcRank) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetThreadInfo(
        /* [in] */ ThreadID threadId,
        /* [out] */ DWORD *pdwWin32ThreadId) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetCurrentThreadID(
        /* [out] */ ThreadID *pThreadId) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetClassIDInfo(
        /* [in] */ ClassID classId,
        /* [out] */ ModuleID *pModuleId,
        /* [out] */ mdTypeDef *pTypeDefToken) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetFunctionInfo(
        /* [in] */ FunctionID functionId,
        /* [out] */ ClassID *pClassId,
        /* [out] */ ModuleID *pModuleId,
        /* [out] */ mdToken *pToken) = 0;

    virtual HRESULT STDMETHODCALLTYPE SetEventMask(
        /* [in] */ DWORD dwEvents) = 0;

    virtual HRESULT STDMETHODCALLTYPE SetEnterLeaveFunctionHooks(
        /* [in] */ void *pFuncEnter,
        /* [in] */ void *pFuncLeave,
        /* [in] */ void *pFuncTailcall) = 0;

    virtual HRESULT STDMETHODCALLTYPE SetFunctionIDMapper(
        /* [in] */ void *pFunc) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetTokenAndMetaDataFromFunction(
        /* [in] */ FunctionID functionId,
        /* [in] */ REFIID riid,
        /* [out] */ IUnknown **ppImport,
        /* [out] */ mdToken *pToken) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetModuleInfo(
        /* [in] */ ModuleID moduleId,
        /* [out] */ BYTE **ppBaseLoadAddress,
        /* [in] */ ULONG cchName,
        /* [out] */ ULONG *pcchName,
        /* [out] */ WCHAR szName[  ],
        /* [out] */ AssemblyID *pAssemblyId) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetModuleMetaData(
        /* [in] */ ModuleID moduleId,
        /* [in] */ DWORD dwOpenFlags,
        /* [in] */ REFIID riid,
        /* [out] */ IUnknown **ppOut) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetILFunctionBody(
        /* [in] */ ModuleID moduleId,
        /* [in] */ mdMethodDef methodId,
        /* [out] */ BYTE **ppMethodHeader,
        /* [out] */ ULONG *pcbMethodSize) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetILFunctionBodyAllocator(
        /* [in] */ ModuleID moduleId,
        /* [out] */ void **ppMalloc) = 0;

    virtual HRESULT STDMETHODCALLTYPE SetILFunctionBody(
        /* [in] */ ModuleID moduleId,
        /* [in] */ mdMethodDef methodId,
        /* [in] */ BYTE *pbNewILMethodHeader) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetAppDomainInfo(
        /* [in] */ AppDomainID appDomainId,
        /* [in] */ ULONG cchName,
        /* [out] */ ULONG *pcchName,
        /* [out] */ WCHAR szName[  ],
        /* [out] */ ProcessID *pProcessId) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetAssemblyInfo(
        /* [in] */ AssemblyID assemblyId,
        /* [in] */ ULONG cchName,
        /* [out] */ ULONG *pcchName,
        /* [out] */ WCHAR szName[  ],
        /* [out] */ AppDomainID *pAppDomainId,
        /* [out] */ ModuleID *pModuleId) = 0;

    virtual HRESULT STDMETHODCALLTYPE SetFunctionReJIT(
        /* [in] */ FunctionID functionId) = 0;

    virtual HRESULT STDMETHODCALLTYPE ForceGC( void) = 0;

    virtual HRESULT STDMETHODCALLTYPE SetILInstrumentedCodeMap(
        /* [in] */ FunctionID functionId,
        /* [in] */ BOOL fStartJit,
        /* [in] */ ULONG cILMapEntries,
        /* [in] */ void *rgILMapEntries) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetInprocInspectionInterface(
        /* [out] */ IUnknown **ppicd) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetInprocInspectionIThisThread(
        /* [out] */ IUnknown **ppicd) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetThreadContext(
        /* [in] */ ThreadID threadId,
        /* [out] */ void *pContextId) = 0; // ContextID

    virtual HRESULT STDMETHODCALLTYPE BeginInprocDebugging(
        /* [in] */ BOOL fThisThreadOnly,
        /* [out] */ DWORD *pdwProfilerContext) = 0;

    virtual HRESULT STDMETHODCALLTYPE EndInprocDebugging(
        /* [in] */ DWORD dwProfilerContext) = 0;

    virtual HRESULT STDMETHODCALLTYPE GetILToNativeMapping(
        /* [in] */ FunctionID functionId,
        /* [in] */ ULONG32 cMap,
        /* [out] */ ULONG32 *pcMap,
        /* [out] */ void *map) = 0;
};

struct ICorProfilerCallback : public IUnknown
{
    virtual HRESULT STDMETHODCALLTYPE Initialize(IUnknown *pICorProfilerInfoUnk) = 0;
    virtual HRESULT STDMETHODCALLTYPE Shutdown() = 0;
    virtual HRESULT STDMETHODCALLTYPE AppDomainCreationStarted(AppDomainID appDomainId) = 0;
    virtual HRESULT STDMETHODCALLTYPE AppDomainCreationFinished(AppDomainID appDomainId, HRESULT hrStatus) = 0;
    virtual HRESULT STDMETHODCALLTYPE AppDomainShutdownStarted(AppDomainID appDomainId) = 0;
    virtual HRESULT STDMETHODCALLTYPE AppDomainShutdownFinished(AppDomainID appDomainId, HRESULT hrStatus) = 0;
    virtual HRESULT STDMETHODCALLTYPE AssemblyLoadStarted(AssemblyID assemblyId) = 0;
    virtual HRESULT STDMETHODCALLTYPE AssemblyLoadFinished(AssemblyID assemblyId, HRESULT hrStatus) = 0;
    virtual HRESULT STDMETHODCALLTYPE AssemblyUnloadStarted(AssemblyID assemblyId) = 0;
    virtual HRESULT STDMETHODCALLTYPE AssemblyUnloadFinished(AssemblyID assemblyId, HRESULT hrStatus) = 0;
    virtual HRESULT STDMETHODCALLTYPE ModuleLoadStarted(ModuleID moduleId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ModuleLoadFinished(ModuleID moduleId, HRESULT hrStatus) = 0;
    virtual HRESULT STDMETHODCALLTYPE ModuleUnloadStarted(ModuleID moduleId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ModuleUnloadFinished(ModuleID moduleId, HRESULT hrStatus) = 0;
    virtual HRESULT STDMETHODCALLTYPE ModuleAttachedToAssembly(ModuleID moduleId, AssemblyID assemblyId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ClassLoadStarted(ClassID classId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ClassLoadFinished(ClassID classId, HRESULT hrStatus) = 0;
    virtual HRESULT STDMETHODCALLTYPE ClassUnloadStarted(ClassID classId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ClassUnloadFinished(ClassID classId, HRESULT hrStatus) = 0;
    virtual HRESULT STDMETHODCALLTYPE FunctionUnloadStarted(FunctionID functionId) = 0;
    virtual HRESULT STDMETHODCALLTYPE JITCompilationStarted(FunctionID functionId, BOOL fIsSafeToBlock) = 0;
    virtual HRESULT STDMETHODCALLTYPE JITCompilationFinished(FunctionID functionId, HRESULT hrStatus, BOOL fIsSafeToBlock) = 0;
    virtual HRESULT STDMETHODCALLTYPE JITCachedFunctionSearchStarted(FunctionID functionId, BOOL *pbUseCachedFunction) = 0;
    virtual HRESULT STDMETHODCALLTYPE JITCachedFunctionSearchFinished(FunctionID functionId, HRESULT hrStatus) = 0;
    virtual HRESULT STDMETHODCALLTYPE JITFunctionPitched(FunctionID functionId) = 0;
    virtual HRESULT STDMETHODCALLTYPE JITInlining(FunctionID callerId, FunctionID calleeId, BOOL *pfShouldInline) = 0;
    virtual HRESULT STDMETHODCALLTYPE ThreadCreated(ThreadID threadId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ThreadDestroyed(ThreadID threadId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ThreadAssignedToOSThread(ThreadID managedThreadId, DWORD osThreadId) = 0;
    virtual HRESULT STDMETHODCALLTYPE RemotingClientInvocationStarted() = 0;
    virtual HRESULT STDMETHODCALLTYPE RemotingClientSendingMessage(GUID *pCookie, BOOL fIsAsync) = 0;
    virtual HRESULT STDMETHODCALLTYPE RemotingClientReceivingReply(GUID *pCookie, BOOL fIsAsync) = 0;
    virtual HRESULT STDMETHODCALLTYPE RemotingClientInvocationFinished() = 0;
    virtual HRESULT STDMETHODCALLTYPE RemotingServerReceivingMessage(GUID *pCookie, BOOL fIsAsync) = 0;
    virtual HRESULT STDMETHODCALLTYPE RemotingServerInvocationStarted() = 0;
    virtual HRESULT STDMETHODCALLTYPE RemotingServerInvocationReturned() = 0;
    virtual HRESULT STDMETHODCALLTYPE RemotingServerSendingReply(GUID *pCookie, BOOL fIsAsync) = 0;
    virtual HRESULT STDMETHODCALLTYPE UnmanagedToManagedTransition(FunctionID functionId, COR_PRF_TRANSITION_REASON reason) = 0;
    virtual HRESULT STDMETHODCALLTYPE ManagedToUnmanagedTransition(FunctionID functionId, COR_PRF_TRANSITION_REASON reason) = 0;
    virtual HRESULT STDMETHODCALLTYPE RuntimeSuspendStarted(COR_PRF_SUSPEND_REASON suspendReason) = 0;
    virtual HRESULT STDMETHODCALLTYPE RuntimeSuspendFinished() = 0;
    virtual HRESULT STDMETHODCALLTYPE RuntimeSuspendAborted() = 0;
    virtual HRESULT STDMETHODCALLTYPE RuntimeThreadSuspended(ThreadID threadId) = 0;
    virtual HRESULT STDMETHODCALLTYPE RuntimeThreadResumed(ThreadID threadId) = 0;
    virtual HRESULT STDMETHODCALLTYPE MovedReferences(ULONG cMovedObjectIDRanges, ObjectID oldObjectIDRangeStart[], ObjectID newObjectIDRangeStart[], ULONG cObjectIDRangeLength[]) = 0;
    virtual HRESULT STDMETHODCALLTYPE ObjectAllocated(ObjectID objectId, ClassID classId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ObjectsAllocatedByClass(ULONG cClassCount, ClassID classIds[], ULONG cObjects[]) = 0;
    virtual HRESULT STDMETHODCALLTYPE ObjectReferences(ObjectID objectId, ClassID classId, ULONG cObjectRefs, ObjectID objectRefIds[]) = 0;
    virtual HRESULT STDMETHODCALLTYPE RootReferences(ULONG cRootRefs, ObjectID rootRefIds[]) = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionThrown(ObjectID thrownObjectId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionSearchFunctionEnter(FunctionID functionId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionSearchFunctionLeave() = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionSearchFilterEnter(FunctionID functionId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionSearchFilterLeave() = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionSearchCatcherFound(FunctionID functionId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionOSHandlerEnter(UINT_PTR __unused) = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionOSHandlerLeave(UINT_PTR __unused) = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionUnwindFunctionEnter(FunctionID functionId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionUnwindFunctionLeave() = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionUnwindFinallyEnter(FunctionID functionId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionUnwindFinallyLeave() = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionCatcherEnter(FunctionID functionId, ObjectID objectId) = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionCatcherLeave() = 0;
    virtual HRESULT STDMETHODCALLTYPE COMClassicVTableCreated(ClassID wrappedClassId, REFGUID implementedIID, void *pVTable, ULONG cSlots) = 0;
    virtual HRESULT STDMETHODCALLTYPE COMClassicVTableDestroyed(ClassID wrappedClassId, REFGUID implementedIID, void *pVTable) = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionCLRCatcherFound() = 0;
    virtual HRESULT STDMETHODCALLTYPE ExceptionCLRCatcherExecute() = 0;
};

// ICorProfilerCallback2
struct ICorProfilerCallback2 : public ICorProfilerCallback
{
    virtual HRESULT STDMETHODCALLTYPE ThreadNameChanged(ThreadID threadId, ULONG cchName, WCHAR name[]) = 0;
    virtual HRESULT STDMETHODCALLTYPE GarbageCollectionStarted(int cGenerations, BOOL generationCollected[], COR_PRF_GC_REASON reason) = 0;
    virtual HRESULT STDMETHODCALLTYPE GarbageCollectionFinished() = 0;
    virtual HRESULT STDMETHODCALLTYPE FinalizeableObjectQueued(DWORD finalizerFlags, ObjectID objectID) = 0;
    virtual HRESULT STDMETHODCALLTYPE RootReferences2(ULONG cRootRefs, ObjectID rootRefIds[], COR_PRF_GC_ROOT_KIND rootKinds[], COR_PRF_GC_ROOT_FLAGS rootFlags[], UINT_PTR rootIds[]) = 0;
    virtual HRESULT STDMETHODCALLTYPE HandleCreated(UINT_PTR handleId, ObjectID initialObjectId) = 0;
    virtual HRESULT STDMETHODCALLTYPE HandleDestroyed(UINT_PTR handleId) = 0;
    virtual HRESULT STDMETHODCALLTYPE InitializeForAttach(IUnknown *pCorProfilerInfoUnk, void *pvClientData, UINT cbClientData) = 0;
    virtual HRESULT STDMETHODCALLTYPE ProfilerAttachFinished() = 0;
    virtual HRESULT STDMETHODCALLTYPE ProfilerDetachSucceeded() = 0;
};

// Event masks
#define COR_PRF_MONITOR_THREADS	( 0x2 )

class Profiler : public ICorProfilerCallback2
{
public:
    Profiler();
    virtual ~Profiler();

    // IUnknown
    HRESULT STDMETHODCALLTYPE QueryInterface(REFIID riid, void **ppvObject) override;
    ULONG STDMETHODCALLTYPE AddRef() override;
    ULONG STDMETHODCALLTYPE Release() override;

    // ICorProfilerCallback (override all)
    virtual HRESULT STDMETHODCALLTYPE Initialize(IUnknown *pICorProfilerInfoUnk) override;
    virtual HRESULT STDMETHODCALLTYPE Shutdown() override;
    virtual HRESULT STDMETHODCALLTYPE AppDomainCreationStarted(AppDomainID appDomainId) override;
    virtual HRESULT STDMETHODCALLTYPE AppDomainCreationFinished(AppDomainID appDomainId, HRESULT hrStatus) override;
    virtual HRESULT STDMETHODCALLTYPE AppDomainShutdownStarted(AppDomainID appDomainId) override;
    virtual HRESULT STDMETHODCALLTYPE AppDomainShutdownFinished(AppDomainID appDomainId, HRESULT hrStatus) override;
    virtual HRESULT STDMETHODCALLTYPE AssemblyLoadStarted(AssemblyID assemblyId) override;
    virtual HRESULT STDMETHODCALLTYPE AssemblyLoadFinished(AssemblyID assemblyId, HRESULT hrStatus) override;
    virtual HRESULT STDMETHODCALLTYPE AssemblyUnloadStarted(AssemblyID assemblyId) override;
    virtual HRESULT STDMETHODCALLTYPE AssemblyUnloadFinished(AssemblyID assemblyId, HRESULT hrStatus) override;
    virtual HRESULT STDMETHODCALLTYPE ModuleLoadStarted(ModuleID moduleId) override;
    virtual HRESULT STDMETHODCALLTYPE ModuleLoadFinished(ModuleID moduleId, HRESULT hrStatus) override;
    virtual HRESULT STDMETHODCALLTYPE ModuleUnloadStarted(ModuleID moduleId) override;
    virtual HRESULT STDMETHODCALLTYPE ModuleUnloadFinished(ModuleID moduleId, HRESULT hrStatus) override;
    virtual HRESULT STDMETHODCALLTYPE ModuleAttachedToAssembly(ModuleID moduleId, AssemblyID assemblyId) override;
    virtual HRESULT STDMETHODCALLTYPE ClassLoadStarted(ClassID classId) override;
    virtual HRESULT STDMETHODCALLTYPE ClassLoadFinished(ClassID classId, HRESULT hrStatus) override;
    virtual HRESULT STDMETHODCALLTYPE ClassUnloadStarted(ClassID classId) override;
    virtual HRESULT STDMETHODCALLTYPE ClassUnloadFinished(ClassID classId, HRESULT hrStatus) override;
    virtual HRESULT STDMETHODCALLTYPE FunctionUnloadStarted(FunctionID functionId) override;
    virtual HRESULT STDMETHODCALLTYPE JITCompilationStarted(FunctionID functionId, BOOL fIsSafeToBlock) override;
    virtual HRESULT STDMETHODCALLTYPE JITCompilationFinished(FunctionID functionId, HRESULT hrStatus, BOOL fIsSafeToBlock) override;
    virtual HRESULT STDMETHODCALLTYPE JITCachedFunctionSearchStarted(FunctionID functionId, BOOL *pbUseCachedFunction) override;
    virtual HRESULT STDMETHODCALLTYPE JITCachedFunctionSearchFinished(FunctionID functionId, HRESULT hrStatus) override;
    virtual HRESULT STDMETHODCALLTYPE JITFunctionPitched(FunctionID functionId) override;
    virtual HRESULT STDMETHODCALLTYPE JITInlining(FunctionID callerId, FunctionID calleeId, BOOL *pfShouldInline) override;
    virtual HRESULT STDMETHODCALLTYPE ThreadCreated(ThreadID threadId) override;
    virtual HRESULT STDMETHODCALLTYPE ThreadDestroyed(ThreadID threadId) override;
    virtual HRESULT STDMETHODCALLTYPE ThreadAssignedToOSThread(ThreadID managedThreadId, DWORD osThreadId) override;
    virtual HRESULT STDMETHODCALLTYPE RemotingClientInvocationStarted() override;
    virtual HRESULT STDMETHODCALLTYPE RemotingClientSendingMessage(GUID *pCookie, BOOL fIsAsync) override;
    virtual HRESULT STDMETHODCALLTYPE RemotingClientReceivingReply(GUID *pCookie, BOOL fIsAsync) override;
    virtual HRESULT STDMETHODCALLTYPE RemotingClientInvocationFinished() override;
    virtual HRESULT STDMETHODCALLTYPE RemotingServerReceivingMessage(GUID *pCookie, BOOL fIsAsync) override;
    virtual HRESULT STDMETHODCALLTYPE RemotingServerInvocationStarted() override;
    virtual HRESULT STDMETHODCALLTYPE RemotingServerInvocationReturned() override;
    virtual HRESULT STDMETHODCALLTYPE RemotingServerSendingReply(GUID *pCookie, BOOL fIsAsync) override;
    virtual HRESULT STDMETHODCALLTYPE UnmanagedToManagedTransition(FunctionID functionId, COR_PRF_TRANSITION_REASON reason) override;
    virtual HRESULT STDMETHODCALLTYPE ManagedToUnmanagedTransition(FunctionID functionId, COR_PRF_TRANSITION_REASON reason) override;
    virtual HRESULT STDMETHODCALLTYPE RuntimeSuspendStarted(COR_PRF_SUSPEND_REASON suspendReason) override;
    virtual HRESULT STDMETHODCALLTYPE RuntimeSuspendFinished() override;
    virtual HRESULT STDMETHODCALLTYPE RuntimeSuspendAborted() override;
    virtual HRESULT STDMETHODCALLTYPE RuntimeThreadSuspended(ThreadID threadId) override;
    virtual HRESULT STDMETHODCALLTYPE RuntimeThreadResumed(ThreadID threadId) override;
    virtual HRESULT STDMETHODCALLTYPE MovedReferences(ULONG cMovedObjectIDRanges, ObjectID oldObjectIDRangeStart[], ObjectID newObjectIDRangeStart[], ULONG cObjectIDRangeLength[]) override;
    virtual HRESULT STDMETHODCALLTYPE ObjectAllocated(ObjectID objectId, ClassID classId) override;
    virtual HRESULT STDMETHODCALLTYPE ObjectsAllocatedByClass(ULONG cClassCount, ClassID classIds[], ULONG cObjects[]) override;
    virtual HRESULT STDMETHODCALLTYPE ObjectReferences(ObjectID objectId, ClassID classId, ULONG cObjectRefs, ObjectID objectRefIds[]) override;
    virtual HRESULT STDMETHODCALLTYPE RootReferences(ULONG cRootRefs, ObjectID rootRefIds[]) override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionThrown(ObjectID thrownObjectId) override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionSearchFunctionEnter(FunctionID functionId) override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionSearchFunctionLeave() override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionSearchFilterEnter(FunctionID functionId) override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionSearchFilterLeave() override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionSearchCatcherFound(FunctionID functionId) override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionOSHandlerEnter(UINT_PTR __unused) override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionOSHandlerLeave(UINT_PTR __unused) override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionUnwindFunctionEnter(FunctionID functionId) override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionUnwindFunctionLeave() override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionUnwindFinallyEnter(FunctionID functionId) override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionUnwindFinallyLeave() override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionCatcherEnter(FunctionID functionId, ObjectID objectId) override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionCatcherLeave() override;
    virtual HRESULT STDMETHODCALLTYPE COMClassicVTableCreated(ClassID wrappedClassId, REFGUID implementedIID, void *pVTable, ULONG cSlots) override;
    virtual HRESULT STDMETHODCALLTYPE COMClassicVTableDestroyed(ClassID wrappedClassId, REFGUID implementedIID, void *pVTable) override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionCLRCatcherFound() override;
    virtual HRESULT STDMETHODCALLTYPE ExceptionCLRCatcherExecute() override;

    // ICorProfilerCallback2 (override all)
    virtual HRESULT STDMETHODCALLTYPE ThreadNameChanged(ThreadID threadId, ULONG cchName, WCHAR name[]) override;
    virtual HRESULT STDMETHODCALLTYPE GarbageCollectionStarted(int cGenerations, BOOL generationCollected[], COR_PRF_GC_REASON reason) override;
    virtual HRESULT STDMETHODCALLTYPE GarbageCollectionFinished() override;
    virtual HRESULT STDMETHODCALLTYPE FinalizeableObjectQueued(DWORD finalizerFlags, ObjectID objectID) override;
    virtual HRESULT STDMETHODCALLTYPE RootReferences2(ULONG cRootRefs, ObjectID rootRefIds[], COR_PRF_GC_ROOT_KIND rootKinds[], COR_PRF_GC_ROOT_FLAGS rootFlags[], UINT_PTR rootIds[]) override;
    virtual HRESULT STDMETHODCALLTYPE HandleCreated(UINT_PTR handleId, ObjectID initialObjectId) override;
    virtual HRESULT STDMETHODCALLTYPE HandleDestroyed(UINT_PTR handleId) override;
    virtual HRESULT STDMETHODCALLTYPE InitializeForAttach(IUnknown *pCorProfilerInfoUnk, void *pvClientData, UINT cbClientData) override;
    virtual HRESULT STDMETHODCALLTYPE ProfilerAttachFinished() override;
    virtual HRESULT STDMETHODCALLTYPE ProfilerDetachSucceeded() override;

private:
    std::atomic<int> _refCount;
    ICorProfilerInfo *_pInfo;
};

// Global callback for C# to register
typedef void (*OnThreadEventCallback)(unsigned long long threadId, int eventType);
extern OnThreadEventCallback g_managedCallback;
