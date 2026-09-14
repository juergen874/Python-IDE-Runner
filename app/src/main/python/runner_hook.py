import sys
import os
import io
import json
import traceback
import base64
import threading
import ctypes
import time
import re
import warnings

# Suppress ResourceWarning and noisy background warnings in mobile runner
warnings.filterwarnings("ignore")

try:
    import matplotlib
    matplotlib.use('Agg')
except Exception:
    pass

_stop_requested = False
_active_thread_id = None

class StdoutRedirector:
    def __init__(self, callback=None):
        self.callback = callback
        self._buffer = io.StringIO()

    def write(self, s):
        if not s:
            return 0
        self._buffer.write(str(s))
        if self.callback is not None:
            try:
                self.callback.onOutput(str(s))
            except Exception:
                pass
        return len(str(s))

    def flush(self):
        pass

    def getvalue(self):
        return self._buffer.getvalue()

class StderrRedirector:
    def __init__(self, callback=None):
        self.callback = callback
        self._buffer = io.StringIO()

    def write(self, s):
        if not s:
            return 0
        self._buffer.write(str(s))
        if self.callback is not None:
            try:
                self.callback.onError(str(s))
            except Exception:
                pass
        return len(str(s))

    def flush(self):
        pass

    def getvalue(self):
        return self._buffer.getvalue()

def request_stop():
    global _stop_requested, _active_thread_id
    _stop_requested = True
    if _active_thread_id is not None:
        try:
            res = ctypes.pythonapi.PyThreadState_SetAsyncExc(
                ctypes.c_ulong(_active_thread_id),
                ctypes.py_object(KeyboardInterrupt)
            )
            if res > 1:
                ctypes.pythonapi.PyThreadState_SetAsyncExc(
                    ctypes.c_ulong(_active_thread_id),
                    ctypes.c_long(0)
                )
        except Exception:
            pass

def _trace_dispatch(frame, event, arg):
    global _stop_requested
    if _stop_requested:
        raise KeyboardInterrupt("Execution terminated by user.")
    return _trace_dispatch

def _extract_embedded_html(code_str):
    """Extract HTML template strings or raw HTML embedded in the python code before execution."""
    results = []
    if not code_str:
        return results

    # Search for triple quoted HTML blocks
    pattern = r'["\']{3}(<!DOCTYPE html[\s\S]*?|<html[\s\S]*?)["\']{3}'
    matches = re.findall(pattern, code_str, re.IGNORECASE)
    for m in matches:
        if m.strip() and m.strip() not in results:
            results.append(m.strip())

    return results

def execute_python_code(code, script_path="", workspace_dir="", callback=None):
    global _stop_requested, _active_thread_id
    _stop_requested = False
    _active_thread_id = threading.get_ident()

    plots_base64 = []
    html_outputs = []

    def notify_visual():
        if callback is not None:
            try:
                callback.onVisualUpdate(json.dumps(plots_base64), json.dumps(html_outputs))
            except Exception:
                pass

    # Extract any embedded HTML template statically so it shows up immediately
    embedded_htmls = _extract_embedded_html(code)
    for eh in embedded_htmls:
        if eh not in html_outputs:
            html_outputs.append(eh)

    if html_outputs:
        notify_visual()

    # Determine script directory and working directory
    script_dir = workspace_dir
    if script_path and os.path.exists(script_path):
        script_dir = os.path.dirname(os.path.abspath(script_path))
    elif workspace_dir and os.path.exists(workspace_dir):
        script_dir = os.path.abspath(workspace_dir)

    if script_dir:
        try:
            os.chdir(script_dir)
        except Exception:
            pass
        if script_dir not in sys.path:
            sys.path.insert(0, script_dir)

    if workspace_dir and workspace_dir not in sys.path:
        sys.path.insert(1, workspace_dir)

    # Matplotlib patch
    has_matplotlib = False
    try:
        import matplotlib
        matplotlib.use('Agg')
        import matplotlib.pyplot as plt
        has_matplotlib = True
        plt.close('all')

        def patched_show(*args, **kwargs):
            fignums = plt.get_fignums()
            for num in fignums:
                try:
                    fig = plt.figure(num)
                    buf = io.BytesIO()
                    fig.savefig(buf, format='png', bbox_inches='tight', dpi=150)
                    buf.seek(0)
                    b64 = base64.b64encode(buf.read()).decode('utf-8')
                    plots_base64.append(b64)
                    buf.close()
                except Exception as ex:
                    sys.stderr.write(f"Error rendering plot {num}: {ex}\n")
            plt.close('all')
            notify_visual()

        plt.show = patched_show
    except Exception:
        pass

    old_stdout = sys.stdout
    old_stderr = sys.stderr

    out_redirect = StdoutRedirector(callback)
    err_redirect = StderrRedirector(callback)

    sys.stdout = out_redirect
    sys.stderr = err_redirect

    script_globals = {
        '__name__': '__main__',
        '__file__': script_path if script_path else '<script.py>',
        '__doc__': None,
    }

    sys.settrace(_trace_dispatch)

    start_time = time.time()
    success = True
    error_text = ""

    try:
        compiled = compile(code, script_path if script_path else '<script.py>', 'exec')
        exec(compiled, script_globals)

        # Check for un-shown plots
        if has_matplotlib:
            try:
                import matplotlib.pyplot as plt
                if plt.get_fignums():
                    plt.show()
            except Exception:
                pass

        # Check globals for HTML variables like HTML_TEMPLATE or HTML
        for g_key, g_val in script_globals.items():
            if isinstance(g_val, str) and ("<html" in g_val.lower() or "<!doctype html" in g_val.lower()):
                if g_val not in html_outputs:
                    html_outputs.append(g_val)
                    notify_visual()

    except KeyboardInterrupt:
        success = False
        error_text = "Script stopped by user."
        err_redirect.write("\n⚠️ [KeyboardInterrupt: Execution stopped by user]\n")
    except Exception as e:
        success = False
        tb = traceback.format_exc()
        error_text = str(e)
        err_redirect.write(tb)
    finally:
        sys.settrace(None)
        sys.stdout = old_stdout
        sys.stderr = old_stderr
        _active_thread_id = None

    elapsed_ms = int((time.time() - start_time) * 1000)
    stdout_result = out_redirect.getvalue()
    stderr_result = err_redirect.getvalue()

    # Check for HTML formatted stdout
    raw_combined = stdout_result
    lower_raw = raw_combined.lower()
    if "<html" in lower_raw or "<!doctype html" in lower_raw or "<svg" in lower_raw or "<div class=" in lower_raw or "<canvas" in lower_raw:
        if stdout_result not in html_outputs:
            html_outputs.append(stdout_result)

    # Check for any generated .html files in script directory
    if script_dir and os.path.exists(script_dir):
        try:
            for fname in os.listdir(script_dir):
                if fname.endswith(".html"):
                    fpath = os.path.join(script_dir, fname)
                    if os.path.getmtime(fpath) >= start_time - 1.0:
                        with open(fpath, "r", encoding="utf-8", errors="ignore") as hf:
                            f_content = hf.read()
                            if f_content not in html_outputs:
                                html_outputs.append(f_content)
        except Exception:
            pass

    notify_visual()

    result_data = {
        "success": success,
        "stdout": stdout_result,
        "stderr": stderr_result,
        "error": error_text,
        "elapsed_ms": elapsed_ms,
        "plots": plots_base64,
        "html_outputs": html_outputs
    }

    return json.dumps(result_data)
