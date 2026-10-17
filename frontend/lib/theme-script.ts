export function themeScript(): string {
  return `try{var t=localStorage.getItem("theme")||"system";var d=t==="dark"||(t==="system"&&window.matchMedia("(prefers-color-scheme: dark)").matches);var r=document.documentElement;if(d){r.classList.add("dark");r.style.colorScheme="dark"}else{r.style.colorScheme="light"}}catch(e){}`;
}
