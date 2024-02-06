# TenessScript Grammar:

$$
\begin{align}
  [\text{statement}] &\to 
  \begin{cases}
    \text{\textbf{expr} [newLine+ \textbf{expr}]*}\\
  \end{cases}\\
  [\text{expr}] &\to
  \begin{cases}
    \text{\textit{identifier} <- \textbf{expr} }\\
    \text{loops}\\
    \text{fun-def}\\
    \text{return \textbf{op-expr}, break, continue}\\
    \textbf{op-expr}
  \end{cases}\\
  [\text{op-expr}] &\to
  \begin{cases}
    \text{\textbf{comp-expr} ((\&, |) \textbf{comp-expr})*}
  \end{cases}\\
  [\text{comp-expr}] &\to 
  \begin{cases}
    \text{!\textbf{comp-expr}}\\
    \text{\textbf{arith-expr} ((=, <, >, <=, >=) \textbf{arith-expr})*}
  \end{cases}\\
  [\text{arith-expr}] &\to
  \begin{cases}
    \text{\textbf{term} ((+, -) \textbf{term})*}
  \end{cases}\\
  [\text{term}] &\to 
  \begin{cases}
    \text{\textbf{factor} ((*, /)\textbf{factor})*}
  \end{cases}\\
  [\text{factor}] &\to
  \begin{cases}
  \text{(+,-) \textbf{power}}
  \end{cases}\\
  [\text{power}] &\to 
  \begin{cases}
  \text{\textbf{atom} (\textbf{\^ factor})*}
  \end{cases}\\
  [\text{atom}] &\to
  \begin{cases}
  \text{\textit{number}}\\
  \text{(\textbf{expr})}\\
  \textit{identifier}\\
  \text{'\textit{string}'}\\
  \text{\textbf{list} (+, -, /, \~ )}\\
  \textbf{funcall}\\
  \textbf{if-expr}\\
  \end{cases}\\
  [\text{for-loop}] &\to 
  \begin{cases}
    \text{for \textit{identifier} <- \textbf{atom} to \textbf {atom} (step \textbf{atom})? (: \textbf{expr}),(\{\textbf{statement}\})}\\
  \end{cases}\\
  [\text{while-loop}] &\to 
  \begin{cases}
    \text{while \textbf{op-expr} (: \textbf{expr}),(\{\textbf{statement}\})}\\
  \end{cases}\\
  [\text{fun-def}] &\to 
  \begin{cases}
    \text{fun \textit{identifier}(\textit{identifier? (, identifer)*}) (: \textbf{expr}),(\{\textbf{statement}\})}
  \end{cases}\\
  [\text{fun-call}] &\to
  \begin{cases}
    \text{\textit{identifier((\textbf{op-expr}(, \textbf{op-expr})*)?)}}
  \end{cases}\\
  \text{if-expr} &\to
  \begin{cases}
  \text{if \textbf{op-expr} (: \textbf{expr}),({\{\textbf{statement}\}}) (else (: \textbf{expr}),({\{\textbf{statement}\}}))?}
  \end{cases}\\
  [\text{list}] &\to
  \begin{cases}
    \text{[\textbf{atom} (, \textbf{atom})*]}
  \end{cases}
\end{align}
 $$