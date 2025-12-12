#include "Profiler.h"
#include <iostream>

class ClassFactory : public IClassFactory
{
public:
    ClassFactory() : _refCount(1) {}

    // IUnknown
    HRESULT STDMETHODCALLTYPE QueryInterface(REFIID riid, void **ppvObject) override
    {
        if (riid == IID_IUnknown || riid == IID_IClassFactory)
        {
            *ppvObject = this;
            AddRef();
            return S_OK;
        }
        *ppvObject = nullptr;
        return E_NOINTERFACE;
    }

    ULONG STDMETHODCALLTYPE AddRef() override
    {
        return ++_refCount;
    }

    ULONG STDMETHODCALLTYPE Release() override
    {
        int count = --_refCount;
        if (count == 0)
        {
            delete this;
        }
        return count;
    }

    // IClassFactory
    HRESULT STDMETHODCALLTYPE CreateInstance(IUnknown *pUnkOuter, REFIID riid, void **ppvObject) override
    {
        if (pUnkOuter != nullptr) return CLASS_E_NOAGGREGATION;

        Profiler *profiler = new Profiler();
        return profiler->QueryInterface(riid, ppvObject);
    }

    HRESULT STDMETHODCALLTYPE LockServer(BOOL fLock) override
    {
        return S_OK;
    }

private:
    std::atomic<int> _refCount;
};

extern "C" HRESULT STDMETHODCALLTYPE DllGetClassObject(REFCLSID rclsid, REFIID riid, LPVOID *ppv)
{
    std::cerr << "[Fray.Profiler] DllGetClassObject called." << std::endl;
    // Log the requested CLSID
    std::cerr << "[Fray.Profiler] Requested CLSID Data1: " << std::hex << rclsid.Data1 << std::dec << std::endl;

    if (rclsid != CLSID_FrayProfiler)
    {
        std::cerr << "[Fray.Profiler] CLSID mismatch." << std::endl;
        return CLASS_E_CLASSNOTAVAILABLE;
    }

    ClassFactory *factory = new ClassFactory();
    return factory->QueryInterface(riid, ppv);
}

extern "C" HRESULT STDMETHODCALLTYPE DllCanUnloadNow()
{
    return S_OK;
}
