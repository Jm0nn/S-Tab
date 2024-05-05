from fastapi import FastAPI
from langchain_openai import ChatOpenAI
from langchain.memory import ConversationSummaryBufferMemory
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.schema.runnable import RunnablePassthrough
from py_eureka_client import eureka_client
from dotenv import load_dotenv
import os
import asyncio

app = FastAPI()

app_name = "gpt"
port = 8003

load_dotenv()
openai_api_key = os.getenv("OPENAI_API_KEY")
eureka_server_url = os.getenv("EUREKA_SERVER_URL")

llm = ChatOpenAI(
    openai_api_key=openai_api_key,
    temperature=0.1
)

loop = asyncio.get_event_loop()

eureka_client.init(eureka_server=eureka_server_url,
                   app_name=app_name,
                   instance_id=app_name,
                   instance_port=port,
                   loop=loop
)

@app.get("/api/gpt")
async def chat(q: str, user_id: int):
    memory_key = f"chat_history_{user_id}"

    memory = ConversationSummaryBufferMemory(
        llm=llm,
        max_token_limit=100,
        memory_key=memory_key,
        return_messages=True
    )

    chat_history = memory.load_memory_variables({})[memory_key]

    prompt = ChatPromptTemplate.from_messages([
        ("system", "당신은 학생들의 질문을 아주 친절하고 자세하게 답변할 수 있는 AI 챗봇이다."),
        MessagesPlaceholder(variable_name=memory_key),
        ("user", "{question}")
    ])

    chain = RunnablePassthrough.assign(chat_history=chat_history) | prompt | llm

    result = chain.invoke({"question": q})

    memory.save_context(
        {"input": q},
        {"output": result.content}
    )

    return {"question": q, "answer": result.content}

if __name__ == '__main__':
    import uvicorn
    uvicorn.run(app="gpt:app", host='0.0.0.0', port=port, loop=loop)