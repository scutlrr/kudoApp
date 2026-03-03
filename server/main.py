import os
import json
import time
import uuid
import shutil
from typing import Optional, List
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel

app = FastAPI(title="KudoApp API")

DATA_DIR = "/opt/kudoapp/data"
LINKS_FILE = os.path.join(DATA_DIR, "links.json")
DOCS_DIR = os.path.join(DATA_DIR, "documents")

os.makedirs(DOCS_DIR, exist_ok=True)


# ========== Models ==========

class LinkItem(BaseModel):
    id: str
    url: str
    title: Optional[str] = None
    timestamp: float


class LinkCreateRequest(BaseModel):
    url: str
    title: Optional[str] = None


class DocItem(BaseModel):
    id: str
    filename: str
    original_name: str
    size: int
    mime_type: str
    timestamp: float


class ChatMessage(BaseModel):
    role: str  # user / assistant
    content: str


class ChatRequest(BaseModel):
    message: str
    link_ids: List[str] = []
    doc_ids: List[str] = []
    history: List[ChatMessage] = []


class ChatResponse(BaseModel):
    reply: str


# ========== Storage Helpers ==========

def _load_links() -> list:
    if not os.path.exists(LINKS_FILE):
        return []
    with open(LINKS_FILE, "r") as f:
        return json.load(f)


def _save_links(links: list):
    with open(LINKS_FILE, "w") as f:
        json.dump(links, f, ensure_ascii=False, indent=2)


def _load_docs_meta() -> list:
    meta_file = os.path.join(DATA_DIR, "documents.json")
    if not os.path.exists(meta_file):
        return []
    with open(meta_file, "r") as f:
        return json.load(f)


def _save_docs_meta(docs: list):
    meta_file = os.path.join(DATA_DIR, "documents.json")
    with open(meta_file, "w") as f:
        json.dump(docs, f, ensure_ascii=False, indent=2)


# ========== Link APIs ==========

@app.post("/api/links", response_model=LinkItem)
def create_link(req: LinkCreateRequest):
    links = _load_links()
    item = LinkItem(
        id=str(uuid.uuid4()),
        url=req.url,
        title=req.title,
        timestamp=time.time()
    )
    links.insert(0, item.model_dump())
    _save_links(links)
    return item


@app.get("/api/links", response_model=List[LinkItem])
def list_links():
    return _load_links()


@app.delete("/api/links/{link_id}")
def delete_link(link_id: str):
    links = _load_links()
    links = [l for l in links if l["id"] != link_id]
    _save_links(links)
    return {"status": "ok"}


@app.delete("/api/links")
def clear_links():
    _save_links([])
    return {"status": "ok"}


# ========== Document APIs ==========

@app.post("/api/documents", response_model=DocItem)
async def upload_document(file: UploadFile = File(...)):
    doc_id = str(uuid.uuid4())
    ext = os.path.splitext(file.filename or "file")[1]
    stored_name = f"{doc_id}{ext}"
    file_path = os.path.join(DOCS_DIR, stored_name)

    with open(file_path, "wb") as f:
        content = await file.read()
        f.write(content)

    doc = DocItem(
        id=doc_id,
        filename=stored_name,
        original_name=file.filename or "unknown",
        size=len(content),
        mime_type=file.content_type or "application/octet-stream",
        timestamp=time.time()
    )

    docs = _load_docs_meta()
    docs.insert(0, doc.model_dump())
    _save_docs_meta(docs)
    return doc


@app.get("/api/documents", response_model=List[DocItem])
def list_documents():
    return _load_docs_meta()


@app.get("/api/documents/{doc_id}/download")
def download_document(doc_id: str):
    docs = _load_docs_meta()
    doc = next((d for d in docs if d["id"] == doc_id), None)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")
    file_path = os.path.join(DOCS_DIR, doc["filename"])
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File not found")
    return FileResponse(file_path, filename=doc["original_name"])


@app.delete("/api/documents/{doc_id}")
def delete_document(doc_id: str):
    docs = _load_docs_meta()
    doc = next((d for d in docs if d["id"] == doc_id), None)
    if doc:
        file_path = os.path.join(DOCS_DIR, doc["filename"])
        if os.path.exists(file_path):
            os.remove(file_path)
        docs = [d for d in docs if d["id"] != doc_id]
        _save_docs_meta(docs)
    return {"status": "ok"}


# ========== Chat API ==========

@app.post("/api/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    """
    对话接口 - 预留大模型API接入点。
    目前返回占位回复，后续替换为实际大模型调用。
    """
    # 收集上下文
    context_parts = []

    # 加载选中的链接
    if req.link_ids:
        links = _load_links()
        selected_links = [l for l in links if l["id"] in req.link_ids]
        for link in selected_links:
            title = link.get("title") or ""
            context_parts.append(f"[链接] {title} {link['url']}")

    # 加载选中的文档信息
    if req.doc_ids:
        docs = _load_docs_meta()
        selected_docs = [d for d in docs if d["id"] in req.doc_ids]
        for doc in selected_docs:
            context_parts.append(f"[文档] {doc['original_name']}")

    # TODO: 接入大模型API
    # 这里预留接口，后续可替换为 OpenAI / Claude / 其他大模型调用
    context_info = "\n".join(context_parts) if context_parts else "无附加资料"

    reply = (
        f"[系统提示] 大模型API尚未接入。\n\n"
        f"收到消息: {req.message}\n"
        f"关联资料:\n{context_info}\n\n"
        f"请配置大模型API后即可使用对话分析功能。"
    )

    return ChatResponse(reply=reply)


# ========== Health Check ==========

@app.get("/api/health")
def health():
    return {"status": "ok", "version": "1.0.0"}
