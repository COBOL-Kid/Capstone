import {BrowserRouter, Route, Routes} from "react-router-dom";
import NavBar from "./Components/NavBar.jsx";
import Landing from "./Pages/Landing.jsx";

function App() {
  return (
      <BrowserRouter>
        <NavBar/>
        <Routes>
          <Route path="/" element={<Landing/>} />
        </Routes>
      </BrowserRouter>
  )
}

export default App
