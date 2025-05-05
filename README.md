# NetBeans Ollama Plugin

Credits to orginal Plugin [https://github.com/Hillrunner2008/netbeans-chatgpt](https://github.com/Hillrunner2008/netbeans-chatgpt)

This plugin allows you to use Ollama and OpenAI APIs to generate chat responses directly in NetBeans. I'll probably add some editor context menu actions to speed up code suggestions, but for now its a simple UI for chat that lives in Netbeans.

![Screenshot](screenshots/OpenAIAndOllama2.png)

### Code block detection

![Screenshot](screenshots/ollamaCodeDetect.png)


### Search Chat History (RAG)
Search is added for the chat history. This can search all the history of chats and give back the response and code based on what is put into the input text area.  Please ensure Ollama has this model -> "nomic-embed-text:latest".

![image](https://github.com/user-attachments/assets/864161ac-d4df-43a6-82cf-53c4a4bebd44)

### Gemini Models Incorporated

// --- Get API Key ---
            String apiKey = System.getenv("GEMINI");
            if (apiKey == null || apiKey.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Error: GEMINI_API_KEY environment variable not set.", "API Key Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            
Gemini is integrated.

        GEMINI_MODELS.add("gemini-2.0-flash");
        GEMINI_MODELS.add("gemini-2.0-flash-lite");
        GEMINI_MODELS.add("gemini-1.5-pro");
        GEMINI_MODELS.add("gemini-1.5-flash");
        GEMINI_MODELS.add("gemini-1.5-flash-8b");
        GEMINI_MODELS.add("gemma-3-1b-it");
        GEMINI_MODELS.add("gemma-3-4b-it");
        GEMINI_MODELS.add("gemma-3-12b-it");
        GEMINI_MODELS.add("gemma-3-27b-it");

![image](https://github.com/user-attachments/assets/65b9ab01-148f-464b-9a4e-c7627549d754)


### Vision OCR options is included

Model used: llama3.2-vision

![image](https://github.com/user-attachments/assets/1db941e5-681c-47c3-bfec-e1a5501d8814)


### Code Quality Verification Task 
A task is added, when clicked, it will detect possible bugs and other issues in code.

![image](https://github.com/user-attachments/assets/d46262ed-2b5a-42bb-b105-41c0210d091a)


![image](https://github.com/user-attachments/assets/56dc296a-b154-4544-840d-9d2781a525b8)

For the detected potential issues, the code is suggested with a click of button:
![image](https://github.com/user-attachments/assets/434c8718-7ffb-4d2b-a259-7d2bbc2fb5be)

Some more operational options are added:
![image](https://github.com/user-attachments/assets/da4a8a82-291f-4a8f-9d3f-736047f72a73)

### Rest Client with LLM Power

Basic Rest Client with Client code generation:
![image](https://github.com/user-attachments/assets/329cd460-e07b-4c20-9028-0b34b5f472bd)

Generated Code :

![image](https://github.com/user-attachments/assets/e40c54d9-0832-4c2d-a10b-a57c6688440b)


### OpenAI Error Message

Incase you key is incorrect for OpenAI, you get this error. This integration is not well tested fot OpenAI Keys, if anyone can test and let me know it works, I will update this section.
![Screenshot](screenshots/OpenAIKeyError.png)

## Installation

1. Clone the code, bulild it, then,
2. In NetBeans, go to `Tools > Plugins`.
3. Click on the `Downloaded` tab.
4. Click on the `Add Plugins...` button and select the compiled plugin.
5. Restart NetBeans

## Customizations
In OS environment variable if "LLM_OLLAMA_HOST" is present, then this will override the url "http://localhost:11434" that points to local Ollama endpoint.

This is what is happening inside plugin code:

static String OLLAMA_EP="http://localhost:11434";
 
static{
        //LLM Settings        
        String value_name = System.getenv("LLM_OLLAMA_HOST");//Get this from environment vaiable to add flexibility to refer to any other Ollama hosting.
        if(value_name!=null) OLLAMA_EP=value_name;
    }
