from fastapi.responses import JSONResponse
from fastapi.encoders import jsonable_encoder # Thêm thư viện này

def success_response(data, message="Thành công", status_code=200):
    return JSONResponse(
        status_code=status_code,
        content={
            "success": True,
            "data": jsonable_encoder(data), # Bọc data bằng jsonable_encoder
            "message": message
        }
    )

def error_response(code: str, message: str, status_code=400):
    return JSONResponse(
        status_code=status_code,
        content={
            "success": False,
            "error": {
                "code": code,
                "message": message
            }
        }
    )